package com.bankpaymentmonitor.paymentservice.payment.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        if (appUserRepository.findByUsername(request.username()).isPresent()) {
            throw new UsernameAlreadyExistsException(request.username());
        }

        AppUser user = new AppUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.branchCode(),
                request.role(),
                true
        );

        AppUser savedUser = appUserRepository.save(user);

        return new UserResponse(
                savedUser.getUsername(),
                savedUser.getBranchCode(),
                savedUser.getRole()
        );
    }
}