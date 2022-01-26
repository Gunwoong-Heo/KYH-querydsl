package study.KYHquerydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.KYHquerydsl.dto.MemberDto;
import study.KYHquerydsl.dto.QMemberDto;
import study.KYHquerydsl.dto.UserDto;
import study.KYHquerydsl.entity.Member;
import study.KYHquerydsl.entity.QMember;
import study.KYHquerydsl.entity.QTeam;
import study.KYHquerydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.TypedQuery;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.KYHquerydsl.entity.QMember.*;
import static study.KYHquerydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;
    JPAQueryFactory queryfactory;

    @BeforeEach
    public void before() {
        queryfactory = new JPAQueryFactory(em);

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

        // 초기화
        em.flush();
        em.clear();
    }

    @Test
    public void startJPQL() {
        // member1을 찾아라.
        String qlString =
                "select m from Member m " +
                "where m.username = :username";

        Member member1 = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(member1.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
//        QMember m = new QMember("m");
//        QMember m = QMember.member;
        Member findMember = queryfactory
                .select(member)  // QMember 를 static import
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member findMember = queryfactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        Member findMember = queryfactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {

        List<Member> fetch = queryfactory
                .selectFrom(member)
                .fetch();
        
        // 결과가 여러개 일 때, NonUniqueResultException 발생
        Member fetchOne = queryfactory
                .selectFrom(member)
                .fetchOne();

        Member fetchFirst = queryfactory
                .selectFrom(member)
                .fetchFirst();

        //  fetchResults() -> Deprecated(향후 미지원)
        QueryResults<Member> results = queryfactory
                .selectFrom(member)
                .fetchResults();
        long total = results.getTotal();
        List<Member> content = results.getResults();

        // fetchCount() -> Deprecated(향후 미지원)
        long total2 = queryfactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단, 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryfactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() {
        List<Member> result = queryfactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2() {
        QueryResults<Member> queryResults = queryfactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    @Test
    public void aggregation() {
        // 여러가지 타입이 반환될때 Tuple로 받을 수 있다.
        // 실무에서는 Tuple 보다 Dto로 뽑아오는 방식을 더 많이 사용한다.
        List<Tuple> result = queryfactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);

        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() {

        List<Tuple> result = queryfactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    @Test
    public void join() {
        List<Member> result = queryfactory
                .selectFrom(member)
                .join(member.team, team)
//                .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     * (약간 억지성 예제이지만, 연관관계 없이 조인 하는 것을 테스트)
     * CROSS JOIN ( Cartesian Product )
     */
    @Test
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryfactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL : select m, t from Member m left join Team t on m.team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering() {
        // select 가 여러가지 타입으로 나오기 때문에 Tuple로 받기
        List<Tuple> result = queryfactory
                .select(member, team)
                .from(member)
//                .leftJoin(member.team, team).on(team.name.eq("teamA"))  // 4건 출력 (2건의 Team은 null)
//                .join(member.team, team).on(team.name.eq("teamA"))  // 2건 출력
//                .join(member.team, team).where(team.name.eq("teamA"))  // 2건 출력
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }



    /**
     * 연관관계 없는 엔티티 외부 조인
     * 회원 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryfactory
                .select(member, team)
                .from(member)
//                .leftJoin(member.team, team)
                .leftJoin(member.team, team).on(member.username.eq(team.name))
//                .leftJoin(team).on(member.username.eq(team.name))  // `leftJoin(team)`을 넣으면 id로 매칭이 되는게 아니라 on절의 조건에 의해서만 조인됨.
//                .join(team).on(team.name.eq(member.username))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = queryfactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryfactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryfactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원 조회
     */
    @Test
    public void subQueryGoe() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryfactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * 나이가 10 초과인 회원 조회
     */
    @Test
    public void subQueryIn() {
        QMember memberSub = new QMember("memberSub");

        // 예제성 억지 쿼리(안 효율적임)
        List<Member> result = queryfactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    public void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");
        List<Tuple> result = queryfactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void basicCase() {
        List<String> result = queryfactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    // db에서 복잡한 분류 작업 등을 하는 것은 권장하지 않음. -> 어플리케이션layer나 화면layer에서 처리하는게 더 나은 선택일 수 있음.
    public void complexCase() {
        List<String> result = queryfactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void constant() {
        List<Tuple> result = queryfactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat() {
        List<String> result = queryfactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void simpleProjection() {
        List<String> result = queryfactory
                .select(member.username)
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection() {
        // Tuple은 repository 계층에서만 쓰는 게 좋다.(service,controller 까지 넘어가는 것은 좋은 설계가 아님)
        List<Tuple> result = queryfactory
                .select(member.username, member.age)
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    public void findDtoByJPQL() {
        List<MemberDto> result = em.createQuery(
                        "select new study.KYHquerydsl.dto.MemberDto(m.username, m.age) " +
                                "from Member m", MemberDto.class)
                .getResultList();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoBySetter() {
        List<MemberDto> result = queryfactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByField() {
        List<MemberDto> result = queryfactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDtoByField() {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryfactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),  // 필드명이 맞아야한다. 다를 때는 `.as()`로 맞춰줘야한다.
//                        member.age
                        ExpressionUtils.as(
                                JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age"  // 필드명 age를 맞춰줌
                        )
                    )
                )
                .from(member)
                .fetch();
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    public void findDtoByConstructor() {
        List<MemberDto> result = queryfactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDtoByConstructor() {
        List<UserDto> result = queryfactory
                .select(Projections.constructor(UserDto.class,
                        member.username,  // 생성자로 들어가니 filed나 property가 같지 않아도 됨.
                        member.age))  // `member.birth`같은 들어가면 안되는 인자를 추가해도 컴파일 시에 오류를 잡아내지 못함.
                .from(member)
                .fetch();
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    // `MemberDto`에 `@QueryProjection` 추가
    // 단점: Q파일 생성해야함, Dto에 querydsl에 의존성을 가지게 됨.(기존에는 clean한 상태였음)
    //       dto는 service,controller 에서도 쓰고 심지어 api로 바로 반환하기도 한다.
    //       그러한 dto안에 querydsl이 들어가게 되면 더러워짐.
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryfactory
                .select(new QMemberDto(member.username, member.age))  // `member.birth`같은 들어가면 안되는 인자를 추가하면 컴파일 시에 오류를 잡아냄. `command + p` 단축키도 잘 먹음
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }




}