package study.KYHquerydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.KYHquerydsl.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByUserName(String username);
}