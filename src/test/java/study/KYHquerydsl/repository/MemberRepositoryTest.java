package study.KYHquerydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.KYHquerydsl.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @Test
    public void basicTest() {
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        Member findMember = memberRepository.findById(member.getId()).get();  // 실무에서는 무턱대고 get으로 꺼내면 안됨.
        assertThat(findMember.getId()).isEqualTo(member.getId());
        assertThat(findMember.getUsername()).isEqualTo(member.getUsername());

//        List<Member> result1 = memberRepository.findAll_Querydsl();
        List<Member> result1 = memberRepository.findAll();
        assertThat(result1).containsExactly(member);

//        List<Member> result2 = memberRepository.findByUserName_Querydsl("member1");
        List<Member> result2 = memberRepository.findByUserName("member1");
        assertThat(result2).containsExactly(member);
    }


}