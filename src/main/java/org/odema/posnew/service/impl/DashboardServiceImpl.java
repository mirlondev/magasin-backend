package org.odema.posnew.service.impl;

import lombok.RequiredArgsConstructor;
import org.odema.posnew.dto.response.*;
import org.odema.posnew.entity.*;
import org.odema.posnew.entity.enums.OrderStatus;
import org.odema.posnew.entity.enums.UserRole;
import org.odema.posnew.exception.NotFoundException;
import org.odema.posnew.repository.*;
import org.odema.posnew.service.DashboardService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ShiftReportRepository shiftReportRepository;
    private final RefundRepository refundRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public DashboardOverviewResponse getDashboardOverview(UUID userId, LocalDate startDate, LocalDate endDate) {
        // Récupérer l'utilisateur et son rôle
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

        UserRole role = user.getUserRole();
        Store assignedStore = user.getAssignedStore();

// ADMIN → vue globale
        if (role == UserRole.ADMIN) {
            return buildAdminOverview(user, startDate, endDate);
        }

// NON-ADMIN → store obligatoire
        if (assignedStore == null) {
            throw new IllegalStateException("Utilisateur sans store assigné");
        }

        // Définir les dates par défaut
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // Données communes
        BigDecimal totalSales = getTotalSales(assignedStore, startDateTime, endDateTime);
        Integer totalOrders = getOrderCount(assignedStore, startDateTime, endDateTime);
        Integer totalCustomers = getCustomerCount(assignedStore, startDateTime, endDateTime);
        Integer totalProducts = getProductCount(assignedStore);

        // Calculer les tendances (vs période précédente)
        LocalDate previousStartDate = startDate.minusDays(ChronoUnit.DAYS.between(startDate, endDate));
        LocalDate previousEndDate = startDate.minusDays(1);
        LocalDateTime previousStartDateTime = previousStartDate.atStartOfDay();
        LocalDateTime previousEndDateTime = previousEndDate.atTime(23, 59, 59);

        BigDecimal previousSales = getTotalSales(assignedStore, previousStartDateTime, previousEndDateTime);
        BigDecimal salesGrowth = calculateGrowthRate(totalSales, previousSales);

        Integer previousOrders = getOrderCount(assignedStore, previousStartDateTime, previousEndDateTime);
        BigDecimal orderGrowth = calculateGrowthRate(
                BigDecimal.valueOf(totalOrders),
                BigDecimal.valueOf(previousOrders)
        );

        Integer previousCustomers = getCustomerCount(assignedStore, previousStartDateTime, previousEndDateTime);
        BigDecimal customerGrowth = calculateGrowthRate(
                BigDecimal.valueOf(totalCustomers),
                BigDecimal.valueOf(previousCustomers)
        );

        // Obtenir les données de graphique
        Map<String, BigDecimal> dailySales = getDailySales(assignedStore, startDateTime, endDateTime);
        Map<String, BigDecimal> weeklySales = getWeeklySales(assignedStore, startDateTime, endDateTime);
        Map<String, BigDecimal> monthlySales = getMonthlySales(assignedStore, startDateTime, endDateTime);

        // Alertes de stock
        List<InventoryAlertResponse> lowStockItems = getInventoryAlerts(
                assignedStore != null ? assignedStore.getStoreId() : null,
                10
        );

        // Commandes en attente
        List<OrderAlertResponse> pendingOrders = getPendingOrders(assignedStore);

        // Activités récentes
        List<ActivityLogResponse> recentActivities = getRecentActivities(userId);

        // Données spécifiques au rôle
        Object roleSpecificData = getRoleSpecificData(role, assignedStore, startDateTime, endDateTime);

        assert assignedStore != null;
        return new DashboardOverviewResponse(
                role,
                assignedStore.getName(),
                startDate,
                endDate,
                totalSales,
                totalOrders,
                totalCustomers,
                totalProducts,
                getActiveEmployeeCount(assignedStore),
                salesGrowth,
                orderGrowth,
                customerGrowth,
                dailySales,
                weeklySales,
                monthlySales,
                lowStockItems,
                pendingOrders,
                recentActivities,
                roleSpecificData
        );
    }

    private Map<String, BigDecimal> getMonthlySales(Store assignedStore, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return null;
    }

    private Map<String, BigDecimal> getWeeklySales(Store assignedStore, LocalDateTime startDateTime, LocalDateTime endDateTime) {
            return null;
    }

    @Override
    public AdminDashboardResponse getAdminDashboard(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // Compter les stores
        List<Store> allStores = storeRepository.findAllByIsActiveTrue();
        Integer totalStores = allStores.size();

        // Compter les utilisateurs
        List<User> allUsers = userRepository.findAll();
        Integer totalUsers = allUsers.size();
        Integer activeUsers = (int) allUsers.stream()
                .filter(User::getActive)
                .count();

        // Calculer le revenu total
        BigDecimal totalRevenue = allStores.stream()
                .map(store -> orderRepository.getTotalSalesByStoreAndDateRange(
                        store.getStoreId(), startDateTime, endDateTime))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculer la valeur moyenne des commandes
        BigDecimal averageOrderValue = calculateAverageOrderValue(null, startDateTime, endDateTime);

        // Obtenir les données par type de store
        Map<String, BigDecimal> revenueByStoreType = getRevenueByStoreType(startDateTime, endDateTime);
        Map<String, Integer> ordersByStoreType = getOrdersByStoreType(startDateTime, endDateTime);

        // Top performers
        List<StorePerformanceResponse> topPerformingStores = getTopPerformingStores(startDateTime, endDateTime, 5);
        List<UserPerformanceResponse> topPerformingUsers = getTopPerformingUsers(startDateTime, endDateTime, 5);

        // Alertes système
        List<SystemAlertResponse> systemAlerts = getSystemAlerts();

        // Tendances de revenu
        Map<String, BigDecimal> revenueTrend = getRevenueTrend(startDate.minusDays(90), endDate);

        return new AdminDashboardResponse(
                totalStores,
                totalStores, // Tous les stores actifs
                totalUsers,
                activeUsers,
                totalRevenue,
                averageOrderValue,
                calculateTotalExpenses(startDateTime, endDateTime),
                calculateNetProfit(startDateTime, endDateTime),
                revenueByStoreType,
                ordersByStoreType,
                topPerformingStores,
                topPerformingUsers,
                systemAlerts,
                revenueTrend,
                getUserGrowth(startDate.minusDays(90), endDate),
                calculateCashFlow(startDateTime, endDateTime),
                calculateTotalInventoryValue(),
                calculateAccountsReceivable(),
                calculateAccountsPayable()
        );
    }

    @Override
    public DepotDashboardResponse getDepotManagerDashboard(UUID storeId, LocalDate startDate, LocalDate endDate) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store non trouvé"));

        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // Compter les produits dans le dépôt
        List<Inventory> inventories = inventoryRepository.findByStore_StoreId(storeId);
        Integer totalProductsInDepot = inventories.size();

        // Calculer la valeur totale de l'inventaire
        BigDecimal totalInventoryValue = inventories.stream()
                .map(Inventory::getTotalValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Obtenir les mouvements de stock
        Map<UUID, Integer> stockMovements = getStockMovements(storeId, startDateTime, endDateTime);

        // Compter les alertes de stock
        List<InventoryAlertResponse> alerts = getInventoryAlerts(storeId, 10);
        Integer lowStockItems = (int) alerts.stream()
                .filter(a -> a.alertLevel().equals("LOW"))
                .count();
        Integer outOfStockItems = (int) alerts.stream()
                .filter(a -> a.alertLevel().equals("OUT_OF_STOCK"))
                .count();
        Integer overStockItems = (int) alerts.stream()
                .filter(a -> a.alertLevel().equals("OVER_STOCK"))
                .count();

        return new DepotDashboardResponse(
                storeId,
                store.getName(),
                startDate,
                endDate,
                totalProductsInDepot,
                totalInventoryValue,
                getPendingTransfersCount(storeId),
                getCompletedTransfersCount(storeId),
                stockMovements,
                getTransferValues(storeId, startDateTime, endDateTime),
                lowStockItems,
                outOfStockItems,
                overStockItems,
                getPendingPurchaseOrdersCount(storeId),
                getPendingPurchaseValue(storeId),
                calculateTurnoverRate(storeId, startDateTime, endDateTime),
                calculateStorageUtilization(storeId),
                calculatePickingAccuracy(storeId, startDateTime, endDateTime),
                getDepotAlerts(storeId),
                getPopularProductsInDepot(storeId, startDateTime, endDateTime, 10),
                getInventoryTrend(storeId, startDate.minusDays(90), endDate),
                getTransferTrend(storeId, startDate.minusDays(90), endDate)
        );
    }

    @Override
    public ShopDashboardResponse getShopManagerDashboard(UUID storeId, LocalDate startDate, LocalDate endDate) {
        Store store1 = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store non trouvé"));
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // Calculer les ventes
        BigDecimal totalSales = getTotalSales(store1, startDateTime, endDateTime);
        Integer totalTransactions = getOrderCount(store1, startDateTime, endDateTime);

        // Obtenir les ventes par caissier
        Map<String, BigDecimal> salesByCashier = getSalesByCashier(storeId, startDateTime, endDateTime);
        Map<String, Integer> transactionsByCashier = getTransactionsByCashier(storeId, startDateTime, endDateTime);

        // Calculer les ventes pour différentes périodes
        BigDecimal salesToday = getTotalSales(store1,
                LocalDate.now().atStartOfDay(),
                LocalDateTime.now()
        );

        BigDecimal salesThisWeek = getTotalSales(store1,
                LocalDate.now().minusDays(7).atStartOfDay(),
                LocalDateTime.now()
        );

        BigDecimal salesThisMonth = getTotalSales(store1,
                LocalDate.now().withDayOfMonth(1).atStartOfDay(),
                LocalDateTime.now()
        );

        // Analyser les heures de pointe
        Map<String, Integer> peakHours = analyzePeakHours(storeId, startDateTime, endDateTime);

        // Métriques clients
        Integer newCustomers = getNewCustomersCount(storeId, startDate, endDate);
        Integer returningCustomers = getReturningCustomersCount(storeId, startDate, endDate);

        // Remboursements
        Integer totalRefunds = refundRepository.countCompletedRefundsByStore(storeId);
        BigDecimal refundAmount = refundRepository.getTotalRefundsByStoreAndDateRange(
                storeId, startDateTime, endDateTime
        );

        // Caisse actuelle
        Optional<ShiftReport> openShift = shiftReportRepository.findOpenShiftsByStore(storeId)
                .stream()
                .findFirst();

        BigDecimal currentCashBalance = openShift.map(ShiftReport::getActualBalance).orElse(BigDecimal.ZERO);
        BigDecimal expectedCashBalance = openShift.map(ShiftReport::getExpectedBalance).orElse(BigDecimal.ZERO);
        BigDecimal cashDiscrepancy = openShift.map(shift ->
                shift.getActualBalance().subtract(shift.getExpectedBalance())
        ).orElse(BigDecimal.ZERO);

        return new ShopDashboardResponse(
                storeId,
                store1.getName(),
                startDate,
                endDate,
                totalSales,
                totalTransactions,
                getCustomerCount(store1, startDateTime, endDateTime),
                calculateAverageOrderValue(store1, startDateTime, endDateTime),
                salesByCashier,
                transactionsByCashier,
                getProductCount(store1),
                getLowStockProductsCount(storeId),
                getOutOfStockProductsCount(storeId),
                salesToday,
                salesThisWeek,
                salesThisMonth,
                peakHours,
                newCustomers,
                returningCustomers,
                calculateCustomerSatisfaction(storeId, startDateTime, endDateTime),
                totalRefunds,
                refundAmount != null ? refundAmount : BigDecimal.ZERO,
                currentCashBalance,
                expectedCashBalance,
                cashDiscrepancy,
                getShopAlerts(storeId)
        );
    }

    @Override
    public <FrequentProductResponse> CashierDashboardResponse getCashierDashboard(UUID storeId, UUID cashierId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store non trouvé"));

        User cashier = userRepository.findById(cashierId)
                .orElseThrow(() -> new NotFoundException("Caissier non trouvé"));

        // Trouver le shift actuel
        Optional<ShiftReport> currentShift = shiftReportRepository.findOpenShiftByCashier(cashierId);

        // Commandes en attente
        List<Order> pendingOrders = orderRepository.findByStore_StoreIdAndStatus(storeId, OrderStatus.PENDING);
        List<Order> processingOrders = orderRepository.findByStore_StoreIdAndStatus(storeId, OrderStatus.PROCESSING);

        // Transactions récentes (dernières 24 heures)
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        List<Order> recentOrders = orderRepository.findByStoreAndDateRange(
                storeId, yesterday, LocalDateTime.now()
        );

        // Produits fréquemment scannés
        List<FrequentProductResponse> frequentProducts = getFrequentProducts(cashierId, storeId, 10);

        return new CashierDashboardResponse(
                cashierId,
                cashier.getUsername(),
                storeId,
                store.getName(),
                currentShift.map(ShiftReport::getShiftReportId).orElse(null),
                currentShift.map(ShiftReport::getStartTime).orElse(null),
                currentShift.map(ShiftReport::getOpeningBalance).orElse(BigDecimal.ZERO),
                currentShift.map(ShiftReport::getActualBalance).orElse(BigDecimal.ZERO),
                currentShift.map(ShiftReport::getTotalTransactions).orElse(0),
                currentShift.map(ShiftReport::getTotalSales).orElse(BigDecimal.ZERO),
                calculateAverageTransactionTime(cashierId, storeId),
                getProductsScannedCount(cashierId, storeId, LocalDate.now()),
                getCustomersServedCount(cashierId, storeId, LocalDate.now()),
                pendingOrders.stream().map(this::convertToOrderResponse).collect(Collectors.toList()),
                processingOrders.stream().map(this::convertToOrderResponse).collect(Collectors.toList()),
                getCashierAlerts(cashierId, storeId),
                getDailySalesTarget(cashierId),
                calculateCurrentSalesProgress(cashierId, storeId),
                getDailyTransactionTarget(cashierId),
                calculateCurrentTransactionProgress(cashierId, storeId),
                recentOrders.stream().map(this::convertToTransactionSummary).collect(Collectors.toList()),
                frequentProducts,
                getQuickActions(cashierId, storeId)
        );
    }

    // Méthodes auxiliaires (implémentations simplifiées)

    private BigDecimal getTotalSales(Store store, LocalDateTime start, LocalDateTime end) {
        if (store == null) {
            // Pour admin, toutes les ventes
            return orderRepository.findAll().stream()
                    .filter(o -> o.getCreatedAt().isAfter(start) && o.getCreatedAt().isBefore(end))
                    .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal sales = orderRepository.getTotalSalesByStoreAndDateRange(
                store.getStoreId(), start, end);
        return sales != null ? sales : BigDecimal.ZERO;
    }

    private Integer getOrderCount(Store store, LocalDateTime start, LocalDateTime end) {
        if (store == null) {
            return (int) orderRepository.findAll().stream()
                    .filter(o -> o.getCreatedAt().isAfter(start) && o.getCreatedAt().isBefore(end))
                    .count();
        }

        Integer count = orderRepository.getOrderCountByStoreAndDateRange(
                store.getStoreId(), start, end);
        return count != null ? count : 0;
    }

    private Integer getCustomerCount(Store store, LocalDateTime start, LocalDateTime end) {
        if (store == null) {
            return customerRepository.countActiveCustomers().intValue();
        }

        // Logique pour compter les clients par store
        return (int) orderRepository.findByStoreAndDateRange(store.getStoreId(), start, end)
                .stream()
                .map(Order::getCustomer)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private Integer getProductCount(Store store) {
        if (store == null) {
            return productRepository.findAllActiveProducts().size();
        }

        return inventoryRepository.findByStore_StoreId(store.getStoreId()).size();
    }

    private Integer getActiveEmployeeCount(Store store) {
        if (!store.getIsActive()) {
            return userRepository.findAll().size();
        }

        return Math.toIntExact(userRepository.countActiveEmployeesByStore(store.getStoreId()));
    }

    private BigDecimal calculateGrowthRate(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0 ?
                    BigDecimal.ZERO : BigDecimal.valueOf(100);
        }

        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private Map<String, BigDecimal> getDailySales(Store store, LocalDateTime start, LocalDateTime end) {
        Map<String, BigDecimal> dailySales = new LinkedHashMap<>();

        // Implémentation simplifiée
        // Dans une vraie implémentation, vous feriez une requête SQL groupée par jour
        return dailySales;
    }

    // Les autres méthodes sont similaires et doivent être implémentées selon vos besoins

    private Object getRoleSpecificData(UserRole role, Store store, LocalDateTime start, LocalDateTime end) {
        return switch (role) {
            case ADMIN -> getAdminDashboard(LocalDate.now().minusDays(30), LocalDate.now());
            case DEPOT_MANAGER -> store != null ?
                    getDepotManagerDashboard(store.getStoreId(),
                            LocalDate.now().minusDays(30), LocalDate.now()) : null;
            case STORE_ADMIN -> store != null ?
                    getShopManagerDashboard(store.getStoreId(),
                            LocalDate.now().minusDays(30), LocalDate.now()) : null;
            case CASHIER -> "Dashboard spécifique au caissier";
            default -> null;
        };
    }

    // Méthodes de conversion
    private OrderResponse convertToOrderResponse(Order order) {
        // Implémenter la conversion
        return null;
    }

    private <transactionSummaryResponse> transactionSummaryResponse convertToTransactionSummary(Order order) {
        // Implémenter la conversion
        return null;
    }

    // Implémenter les autres méthodes requises par l'interface...

    @Override
    public SalesChartResponse getSalesChartData(UUID storeId, LocalDate startDate, LocalDate endDate, String period) {
        // Implémentation de la méthode
        return null;
    }

    @Override
    public List<TopProductResponse> getTopProducts(UUID storeId, LocalDate startDate, LocalDate endDate, int limit) {
        // Implémentation de la méthode
        return Collections.emptyList();
    }

    @Override
    public List<InventoryAlertResponse> getInventoryAlerts(UUID storeId, int threshold) {
        // Implémentation de la méthode
        return Collections.emptyList();
    }

    @Override
    public PerformanceMetricsResponse getPerformanceMetrics(UUID storeId, LocalDate startDate, LocalDate endDate) {
        // Implémentation de la méthode
        return null;
    }

    @Override
    public List<ActivityLogResponse> getRecentActivities(UUID userId) {
        // Implémentation de la méthode
        return Collections.emptyList();
    }

    @Override
    public BigDecimal calculateSalesGrowth(UUID storeId, LocalDate currentStart, LocalDate currentEnd,
                                           LocalDate previousStart, LocalDate previousEnd) {
        // Implémentation de la méthode
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getNewCustomersCount(UUID storeId, LocalDate startDate, LocalDate endDate) {
        // Implémentation de la méthode
        return 0;
    }

    @Override
    public BigDecimal getAverageOrderValue(UUID storeId, LocalDate startDate, LocalDate endDate) {
        // Implémentation de la méthode
        return BigDecimal.ZERO;
    }

    @Override
    public Map<String, BigDecimal> getRevenueByCategory(UUID storeId, LocalDate startDate, LocalDate endDate) {
        // Implémentation de la méthode
        return Collections.emptyMap();
    }

    // Méthodes privées auxiliaires (à implémenter)
    private BigDecimal calculateAverageOrderValue(Store store, LocalDateTime start, LocalDateTime end) {
        return BigDecimal.ZERO;
    }

    private Map<String, BigDecimal> getRevenueByStoreType(LocalDateTime start, LocalDateTime end) {
        return Collections.emptyMap();
    }

    private Map<String, Integer> getOrdersByStoreType(LocalDateTime start, LocalDateTime end) {
        return Collections.emptyMap();
    }

    private <StorePerformanceResponse> List<StorePerformanceResponse> getTopPerformingStores(LocalDateTime start, LocalDateTime end, int limit) {
        return Collections.emptyList();
    }

    private List<UserPerformanceResponse> getTopPerformingUsers(LocalDateTime start, LocalDateTime end, int limit) {
        return Collections.emptyList();
    }

    private List<SystemAlertResponse> getSystemAlerts() {
        return Collections.emptyList();
    }

    private Map<String, BigDecimal> getRevenueTrend(LocalDate start, LocalDate end) {
        return Collections.emptyMap();
    }

    private Map<String, Integer> getUserGrowth(LocalDate start, LocalDate end) {
        return Collections.emptyMap();
    }

    private BigDecimal calculateTotalExpenses(LocalDateTime start, LocalDateTime end) {
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateNetProfit(LocalDateTime start, LocalDateTime end) {
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateCashFlow(LocalDateTime start, LocalDateTime end) {
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateTotalInventoryValue() {
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateAccountsReceivable() {
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateAccountsPayable() {
        return BigDecimal.ZERO;
    }

    private Map<UUID, Integer> getStockMovements(UUID storeId, LocalDateTime start, LocalDateTime end) {
        return Collections.emptyMap();
    }

    private Map<UUID, BigDecimal> getTransferValues(UUID storeId, LocalDateTime start, LocalDateTime end) {
        return Collections.emptyMap();
    }

    private Integer getPendingTransfersCount(UUID storeId) {
        return 0;
    }

    private Integer getCompletedTransfersCount(UUID storeId) {
        return 0;
    }

    private Integer getPendingPurchaseOrdersCount(UUID storeId) {
        return 0;
    }

    private BigDecimal getPendingPurchaseValue(UUID storeId) {
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateTurnoverRate(UUID storeId, LocalDateTime start, LocalDateTime end) {
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateStorageUtilization(UUID storeId) {
        return BigDecimal.ZERO;
    }

    private BigDecimal calculatePickingAccuracy(UUID storeId, LocalDateTime start, LocalDateTime end) {
        return BigDecimal.ZERO;
    }

    private List<DepotAlertResponse> getDepotAlerts(UUID storeId) {
        return Collections.emptyList();
    }

    private List<TopProductResponse> getPopularProductsInDepot(UUID storeId, LocalDateTime start,
                                                               LocalDateTime end, int limit) {
        return Collections.emptyList();
    }

    private Map<String, BigDecimal> getInventoryTrend(UUID storeId, LocalDate start, LocalDate end) {
        return Collections.emptyMap();
    }

    private Map<String, Integer> getTransferTrend(UUID storeId, LocalDate start, LocalDate end) {
        return Collections.emptyMap();
    }

    private Map<String, BigDecimal> getSalesByCashier(UUID storeId, LocalDateTime start, LocalDateTime end) {
        return Collections.emptyMap();
    }

    private Map<String, Integer> getTransactionsByCashier(UUID storeId, LocalDateTime start, LocalDateTime end) {
        return Collections.emptyMap();
    }

    private Integer getLowStockProductsCount(UUID storeId) {
        return 0;
    }

    private Integer getOutOfStockProductsCount(UUID storeId) {
        return 0;
    }

    private Map<String, Integer> analyzePeakHours(UUID storeId, LocalDateTime start, LocalDateTime end) {
        return Collections.emptyMap();
    }

    private Integer getReturningCustomersCount(UUID storeId, LocalDate start, LocalDate end) {
        return 0;
    }

    private BigDecimal calculateCustomerSatisfaction(UUID storeId, LocalDateTime start, LocalDateTime end) {
        return BigDecimal.ZERO;
    }

    private List<ShopAlertResponse> getShopAlerts(UUID storeId) {
        return Collections.emptyList();
    }

    private <FrequentProductResponse> List<FrequentProductResponse> getFrequentProducts(UUID cashierId, UUID storeId, int limit) {
        return Collections.emptyList();
    }

    private BigDecimal calculateAverageTransactionTime(UUID cashierId, UUID storeId) {
        return BigDecimal.ZERO;
    }

    private Integer getProductsScannedCount(UUID cashierId, UUID storeId, LocalDate date) {
        return 0;
    }

    private Integer getCustomersServedCount(UUID cashierId, UUID storeId, LocalDate date) {
        return 0;
    }

    private List<CashierAlertResponse> getCashierAlerts(UUID cashierId, UUID storeId) {
        return Collections.emptyList();
    }

    private BigDecimal getDailySalesTarget(UUID cashierId) {
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateCurrentSalesProgress(UUID cashierId, UUID storeId) {
        return BigDecimal.ZERO;
    }

    private Integer getDailyTransactionTarget(UUID cashierId) {
        return 0;
    }

    private Integer calculateCurrentTransactionProgress(UUID cashierId, UUID storeId) {
        return 0;
    }

    private <QuickActionResponse> List<QuickActionResponse> getQuickActions(UUID cashierId, UUID storeId) {
        return Collections.emptyList();
    }

    private List<OrderAlertResponse> getPendingOrders(Store store) {
        return Collections.emptyList();
    }



    private DashboardOverviewResponse buildAdminOverview(
            User user,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        BigDecimal totalSales = getTotalSales(null, start, end);
        Integer totalOrders = getOrderCount(null, start, end);
        Integer totalCustomers = getCustomerCount(null, start, end);
        Integer totalProducts = getProductCount(null);

        return new DashboardOverviewResponse(
                UserRole.ADMIN,
                "GLOBAL",                // ✅ pas de store
                startDate,
                endDate,
                totalSales,
                totalOrders,
                totalCustomers,
                totalProducts,
                userRepository.findAll().size(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Map.of(),
                Map.of(),
                Map.of(),
                getInventoryAlerts(null, 10), // tous les stores
                List.of(),
                getRecentActivities(user.getUserId()),
                getAdminDashboard(startDate, endDate)
        );
    }

}