package study.KYHquerydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryFactory;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.KYHquerydsl.dto.MemberSearchCondition;
import study.KYHquerydsl.dto.MemberTeamDto;
import study.KYHquerydsl.dto.QMemberDto;
import study.KYHquerydsl.dto.QMemberTeamDto;
import study.KYHquerydsl.entity.Member;
import study.KYHquerydsl.entity.QMember;
import study.KYHquerydsl.entity.QTeam;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static org.springframework.util.StringUtils.*;
import static study.KYHquerydsl.entity.QMember.*;
import static study.KYHquerydsl.entity.QTeam.*;

@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

/*
    // 생성자에서 JPAQueryFactory를 생성해서 넣는 방법
    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }
*/

    // JPAQueryFactory 를 @Bean 으로 등록한 후 주입받는 방법
    // @RequiredArgsConstructor 붙임이고 생략가능 (final 붙은 필드는 반드시 초기화가 필요함)
    // 주입받아야 하는 인자가 2개이다보니, 테스트코드 짜거나 할때 조금 더 귀찮아질 수는 있다.
/*
    public MemberJpaRepository(EntityManager em, JPAQueryFactory queryFactory) {
        this.em = em;
        this.queryFactory = queryFactory;
    }
*/

/*
    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);  // 동시성 문제는 em에 의존. em은 Spring과 엮어서 쓰면, 동시성 문제와 관계없이, transaction 단위로, 다 따로따로 분리되서 동작하게 설계됨.
    }
*/

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findAll_Querydsl() {
        return queryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByUserName(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }

    public List<Member> findByUserName_Querydsl(String username) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {

        BooleanBuilder builder = new BooleanBuilder();
        if(hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }
        if (hasText(condition.getTeamName())) {
            builder.and(member.team.name.eq(condition.getTeamName()));
        }
        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        member.team.id.as("memberId"),
                        member.team.name.as("teanName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }

    // Where 다중 파라미터 사용
/*
    public List<Member> search(MemberSearchCondition condition) {
*/
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        member.team.id.as("memberId"),
                        member.team.name.as("teanName")
                ))
                .from(member)
/*
                .selectFrom(member)
*/
                .leftJoin(member.team, team)
                // select Projection 이 달라져도 조건을 재사용 가능하다.
                .where(
                        usernameEq(condition.getUsername()),
                        teamnameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
/*
                        ageBetween(condition.getAgeGoe(), condition.getAgeLoe())
*/
                )
                .fetch();
    }

    // 반환타입을 `Predicate` 보다 `BooleanExpression` 으로 하는 것이 향후 확장성이 좋다. composition이 가능하다.(결합) (`QuerydslBasicTest`의 `allEq` 참고)

    private BooleanExpression usernameEq(String usernameCond) {
//        return usernameCond != null ? member.username.eq(usernameCond) : null;
        return hasText(usernameCond) ? member.username.eq(usernameCond) : null;  // 공백문자가 넘어오는 것까지 체크
    }
    private BooleanExpression teamnameEq(String teamNameCond) {
//        return teamNameCond != null ? member.username.eq(teamNameCond) : null;
        return hasText(teamNameCond) ? member.team.name.eq(teamNameCond) : null;  // 공백문자가 넘어오는 것까지 체크
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

    // 이렇게 원하는 부분만 조립하거나, 전체를 다 조립할 수도 있음. 이 때, null체크는 따로 잘 해줘야함
    private BooleanExpression ageBetween(Integer ageGoe, Integer ageLoe) {
        return ageGoe(ageGoe).and(ageLoe(ageLoe));
    }


}