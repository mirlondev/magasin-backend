package org.odema.posnew.application.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.api.exception.BusinessException;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.application.dto.CustomerResponse;
import org.odema.posnew.application.dto.request.CustomerRequest;
import org.odema.posnew.application.dto.request.DiscountResult;
import org.odema.posnew.application.dto.response.LoyaltySummaryResponse;
import org.odema.posnew.application.dto.response.LoyaltyTransactionResponse;
import org.odema.posnew.application.mapper.CustomerMapper;
import org.odema.posnew.domain.model.Customer;
import org.odema.posnew.domain.model.LoyaltyTransaction;
import org.odema.posnew.domain.model.enums.LoyaltyTier;
import org.odema.posnew.domain.repository.CustomerRepository;
import org.odema.posnew.domain.repository.LoyaltyTransactionRepository;
import org.odema.posnew.domain.service.CustomerService;
import org.odema.posnew.domain.service.LoyaltyService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final LoyaltyTransactionRepository transactionRepository;
    private final CustomerMapper customerMapper;
    private final LoyaltyService loyaltyService;
    private static final int POINTS_PER_CURRENCY_UNIT = 1;
    private static final int POINTS_TO_CURRENCY_RATE = 100;

    @Override
    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        // Vérifier l'unicité de l'email
        if (customerRepository.existsByEmail(request.email())) {
            throw new BusinessException("Un client avec cet email existe déjà");
        }

        // Vérifier l'unicité du téléphone
        if (customerRepository.existsByPhone(request.phone())) {
            throw new BusinessException("Un client avec ce numéro de téléphone existe déjà");
        }

        Customer customer = customerMapper.toEntity(request);
        customer.setLoyaltyPoints(0);
        customer.setLoyaltyTier(LoyaltyTier.BRONZE);
        customer.setTotalPurchases(BigDecimal.ZERO);
        customer.setPurchaseCount(0);
        customer.setIsActive(true);

        Customer savedCustomer = customerRepository.save(customer);
        return customerMapper.toResponse(savedCustomer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Client non trouvé"));

        return customerMapper.toResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByEmail(String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Client non trouvé avec cet email"));

        return customerMapper.toResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByPhone(String phone) {
        Customer customer = customerRepository.findByPhone(phone)
                .orElseThrow(() -> new NotFoundException("Client non trouvé avec ce numéro de téléphone"));

        return customerMapper.toResponse(customer);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(UUID customerId, CustomerRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Client non trouvé"));

        // Vérifier l'unicité de l'email si modifié
        if (request.email() != null && !request.email().equals(customer.getEmail())) {
            if (customerRepository.existsByEmail(request.email())) {
                throw new BusinessException("Un client avec cet email existe déjà");
            }
            customer.setEmail(request.email());
        }

        // Vérifier l'unicité du téléphone si modifié
        if (request.phone() != null && !request.phone().equals(customer.getPhone())) {
            if (customerRepository.existsByPhone(request.phone())) {
                throw new BusinessException("Un client avec ce numéro de téléphone existe déjà");
            }
            customer.setPhone(request.phone());
        }

        // Mettre à jour les autres champs
        if (request.firstName() != null) customer.setFirstName(request.firstName());
        if (request.lastName() != null) customer.setLastName(request.lastName());
        if (request.address() != null) customer.setAddress(request.address());
        if (request.city() != null) customer.setCity(request.city());
        if (request.postalCode() != null) customer.setPostalCode(request.postalCode());
        if (request.country() != null) customer.setCountry(request.country());
        if (request.dateOfBirth() != null) customer.setDateOfBirth(request.dateOfBirth());

        Customer updatedCustomer = customerRepository.save(customer);
        return customerMapper.toResponse(updatedCustomer);
    }

    @Override
    @Transactional
    public void deactivateCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Client non trouvé"));

        if (!customer.getIsActive()) {
            throw new BusinessException("Le client est déjà désactivé");
        }

        customer.setIsActive(false);
        customerRepository.save(customer);
    }

    @Override
    @Transactional
    public void activateCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Client non trouvé"));

        if (customer.getIsActive()) {
            throw new BusinessException("Le client est déjà activé");
        }

        customer.setIsActive(true);
        customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findByIsActiveTrue().stream()
                .map(customerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> searchCustomers(String keyword) {
        return customerRepository.searchCustomers(keyword).stream()
                .map(customerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getTopCustomers(int limit) {
        return customerRepository.findTopCustomers().stream()
                .limit(limit)
                .map(customerMapper::toResponse)
                .toList();
    }


    @Transactional
    @Override
    public DiscountResult usePointsForDiscount(UUID customerId, int points, UUID orderId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Client non trouvé"));

        if (points > customer.getLoyaltyPoints()) {
            throw new BusinessException("Points insuffisants. Disponible: " +
                    customer.getLoyaltyPoints());
        }

        BigDecimal discountValue = convertPointsToCurrency(points);

        // Créer transaction de débit
        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .customer(customer)
                .pointsChange(-points)
                .newBalance(customer.getLoyaltyPoints() - points)
                .reason("Utilisation remise commande " + orderId)
                .orderId(orderId)
                .transactionDate(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);

        // Mettre à jour client
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() - points);
        customerRepository.save(customer);

        log.info("Customer {} used {} points for {} discount",
                customerId, points, discountValue);

        return new DiscountResult(
                BigDecimal.ZERO,
                discountValue,
                BigDecimal.ZERO,
                discountValue,
                BigDecimal.ZERO.subtract(discountValue),
                "LOYALTY",
                "Remise fidélité " + points + " points",
                points,
                0
        );
    }



    @Override
    public int calculatePointsEarned(BigDecimal amount, LoyaltyTier tier) {
        int base = amount.intValue();
        return base + calculateTierBonus(base, tier);
    }


    @Transactional
    @Override
    public void adjustPointsManually(UUID customerId, int delta, String reason, UUID adminId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Client non trouvé"));

        int newBalance = customer.getLoyaltyPoints() + delta;
        if (newBalance < 0) {
            throw new BusinessException("Le solde ne peut pas être négatif");
        }

        customer.setLoyaltyPoints(newBalance);

        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .customer(customer)
                .pointsChange(delta)
                .newBalance(newBalance)
                .reason("Ajustement manuel: " + reason + " (par admin: " + adminId + ")")
                .transactionDate(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
        customerRepository.save(customer);
    }

    // Méthodes utilitaires
    private BigDecimal convertPointsToCurrency(int points) {
        return BigDecimal.valueOf(points / (double) POINTS_TO_CURRENCY_RATE);
    }

    private int calculateTierBonus(int basePoints, LoyaltyTier tier) {
        return switch (tier) {
            case BRONZE -> 0;
            case SILVER -> (int) (basePoints * 0.1);
            case GOLD -> (int) (basePoints * 0.25);
            case PLATINUM -> (int) (basePoints * 0.5);
        };
    }

    private int calculatePointsToNextTier(Customer customer) {
        return getLoyaltier(customer);
    }

    static int getLoyaltier(Customer customer) {
        LoyaltyTier current = customer.getLoyaltyTier();
        if (current == LoyaltyTier.PLATINUM) return 0;

        LoyaltyTier next = LoyaltyTier.values()[current.ordinal() + 1];
        BigDecimal currentPurchases = customer.getTotalPurchases();
        BigDecimal needed = next.getThreshold().subtract(currentPurchases);

        return needed.max(BigDecimal.ZERO).intValue();
    }

    private LoyaltyTransactionResponse mapToTransactionResponse(LoyaltyTransaction tx) {
        return new LoyaltyTransactionResponse(
                tx.getTransactionId(),
                tx.getPointsChange(),
                tx.getNewBalance(),
                tx.getReason(),
                tx.getOrderId(),
                tx.getTransactionDate()
        );
    }

    @Override
    @Transactional
    public CustomerResponse addLoyaltyPoints(UUID customerId, Integer points) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Client non trouvé"));

        if (points <= 0) throw new BusinessException("Le nombre de points doit être positif");

        // ✅ 3 arguments requis par Customer.addLoyaltyPoints()
        customer.addLoyaltyPoints(points, "Ajout manuel de points", null);

        // LoyaltyTransaction déjà créée dans Customer.addLoyaltyPoints() via loyaltyHistory
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public CustomerResponse removeLoyaltyPoints(UUID customerId, Integer points) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Client non trouvé"));

        if (points <= 0) throw new BusinessException("Le nombre de points doit être positif");
        if (customer.getLoyaltyPoints() < points)
            throw new BusinessException("Le client n'a pas suffisamment de points");

        customer.useLoyaltyPoints(points, "Retrait manuel de points", null);

        return customerMapper.toResponse(customerRepository.save(customer));
    }

    // ✅ getCustomerLoyalty — déléguer à LoyaltyService
    @Transactional(readOnly = true)
    @Override
    public LoyaltySummaryResponse getCustomerLoyalty(UUID customerId) {
        return loyaltyService.getCustomerLoyalty(customerId);
    }
}