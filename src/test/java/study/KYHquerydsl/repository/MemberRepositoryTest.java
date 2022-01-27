package study.KYHquerydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import study.KYHquerydsl.dto.MemberSearchCondition;
import study.KYHquerydsl.dto.MemberTeamDto;
import study.KYHquerydsl.entity.Member;
import study.KYHquerydsl.entity.Team;

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
        List<Member> result2 = memberRepository.findByUsername("member1");
        assertThat(result2).containsExactly(member);
    }

    @Test
    public void searchTest() throws Exception {
        // given
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        // 검색조건이 아예 없으면 모든 데이터를 다 긁어온다. (아예 없거나, 적은 범위 지정만 되었을 경우?)
        // 하루에 1000건씩만 쌓여도 1달이면 3만건인데, 그 데이터를 다 긁어온다.
        // 테스트할때는 데이터가 없거나 적으면 문제가 되지 않지만, 실무에서는 문제 생길수도 있다.
        // 반드시 기본조건이나 limit 정도는 걸어두는 것이 좋다. ( 가급적이면 페이징 쿼리가 함께 들어가주는 것이 좋다)
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        // when
//        List<MemberTeamDto> result = memberRepository.searchByBuilder(condition);  // BooleanBuilder 사용
        List<MemberTeamDto> result = memberRepository.search(condition);  // Where 다중 파라미터 사용
/*
        List<Member> result = memberRepository.search(condition);  // Where 다중 파라미터 사용
*/

        // then
        assertThat(result).extracting("username").containsExactly("member4");
    }

    @Test
    public void searchPageSimple() throws Exception {
        // given
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        // 검색조건이 아예 없으면 모든 데이터를 다 긁어온다. (아예 없거나, 적은 범위 지정만 되었을 경우?)
        // 하루에 1000건씩만 쌓여도 1달이면 3만건인데, 그 데이터를 다 긁어온다.
        // 테스트할때는 데이터가 없거나 적으면 문제가 되지 않지만, 실무에서는 문제 생길수도 있다.
        // 반드시 기본조건이나 limit 정도는 걸어두는 것이 좋다. ( 가급적이면 페이징 쿼리가 함께 들어가주는 것이 좋다)
//        condition.setAgeGoe(35);
//        condition.setAgeLoe(40);
//        condition.setTeamName("teamB");
        PageRequest pageRequest = PageRequest.of(0, 3);

        // when
        Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition, pageRequest);

        // then
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getContent()).extracting("username").containsExactly("member1", "member2", "member3");

    }



}