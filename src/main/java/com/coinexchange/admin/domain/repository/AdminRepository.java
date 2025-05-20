package com.coinexchange.admin.domain.repository;

import com.coinexchange.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<User, Long> {
}
