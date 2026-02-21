package org.odema.posnew.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.odema.posnew.application.dto.request.CustomerRequest;
import org.odema.posnew.application.dto.response.CustomerResponse;
import org.odema.posnew.api.exception.BadRequestException;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.application.mapper.CustomerMapper;
import org.odema.posnew.repository.CustomerRepository;
import org.odema.posnew.application.service.CustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        // Vérifier l'unicité de l'email
        if (customerRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Un client avec cet email existe déjà");
        }

        // Vérifier l'unicité du téléphone
        if (customerRepository.existsByPhone(request.phone())) {
            throw new BadRequestException("Un client avec ce numéro de téléphone existe déjà");
        }

        Customer customer = customerMapper.toEntity(request);
        Customer savedCustomer = customerRepository.save(customer);

        return customerMapper.toResponse(savedCustomer);
    }

    @Override
    public CustomerResponse getCustomerById(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Client non trouvé"));

        return customerMapper.toResponse(customer);
    }

    @Override
    public CustomerResponse getCustomerByEmail(String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Client non trouvé avec cet email"));

        return customerMapper.toResponse(customer);
    }

    @Override
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
                throw new BadRequestException("Un client avec cet email existe déjà");
            }
            customer.setEmail(request.email());
        }

        // Vérifier l'unicité du téléphone si modifié
        if (request.phone() != null && !request.phone().equals(customer.getPhone())) {
            if (customerRepository.existsByPhone(request.phone())) {
                throw new BadRequestException("Un client avec ce numéro de téléphone existe déjà");
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

        Customer updatedCustomer = customerRepository.save(customer);
        return customerMapper.toResponse(updatedCustomer);
    }

    @Override
    @Transactional
    public void deactivateCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Client non trouvé"));

        if (!customer.getIsActive()) {
            throw new BadRequestException("Le client est déjà désactivé");
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
            throw new BadRequestException("Le client est déjà activé");
        }

        customer.setIsActive(true);
        customerRepository.save(customer);
    }

    @Override
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findByIsActiveTrue().stream()
                .map(customerMapper::toResponse)
                .toList();
    }

    @Override
    public List<CustomerResponse> searchCustomers(String keyword) {
        return customerRepository.searchCustomers(keyword).stream()
                .map(customerMapper::toResponse)
                .toList();
    }

    @Override
    public List<CustomerResponse> getTopCustomers(int limit) {
        return customerRepository.findTopCustomers().stream()
                .limit(limit)
                .map(customerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CustomerResponse addLoyaltyPoints(UUID customerId, Integer points) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Client non trouvé"));

        if (points <= 0) {
            throw new BadRequestException("Le nombre de points doit être positif");
        }

        customer.addLoyaltyPoints(points);
        Customer updatedCustomer = customerRepository.save(customer);

        return customerMapper.toResponse(updatedCustomer);
    }

    @Override
    @Transactional
    public CustomerResponse removeLoyaltyPoints(UUID customerId, Integer points) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Client non trouvé"));

        if (points <= 0) {
            throw new BadRequestException("Le nombre de points doit être positif");
        }

        if (customer.getLoyaltyPoints() < points) {
            throw new BadRequestException("Le client n'a pas suffisamment de points");
        }

        customer.removeLoyaltyPoints(points);
        Customer updatedCustomer = customerRepository.save(customer);

        return customerMapper.toResponse(updatedCustomer);
    }
}
