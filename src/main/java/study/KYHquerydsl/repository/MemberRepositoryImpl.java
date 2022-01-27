package study.KYHquerydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import study.KYHquerydsl.dto.MemberSearchCondition;
import study.KYHquerydsl.dto.MemberTeamDto;
import study.KYHquerydsl.dto.QMemberTeamDto;

import javax.persistence.EntityManager;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.KYHquerydsl.entity.QMember.member;
import static study.KYHquerydsl.entity.QTeam.team;

// `MemberRepository+Impl` 으로 이름을 맞춰야함.
// `dataJpa`강의내용 : 구현체에서 `리포지토리이름+Impl` 을 반드시 맞춰야한다. (Imple 대신 다른 이름으로 변경하고 싶으면, `XML설정이나,JavaConfig`로 해야한다(메뉴얼참조). 하지만 웬만하면 관례를 따르는게 낫다.)
// `dataJpa`보충내용 : 스프링 데이터 2.x 부터는 사용자 정의 구현 클래스에 리포지토리 인터페이스 이름 + Impl 을 적용하는
//            대신에 사용자 정의 인터페이스 명 + Impl 방식도 지원한다.
//            예를 들어서 위 예제의 MemberRepositoryImpl 대신에 MemberRepositoryCustomImpl 같이 구현해도 된다.
//            기존 방식보다 이 방식이 사용자 정의 인터페이스 이름과 구현 클래스 이름이 비슷하므로 더 직관적이다.
//            추가로 여러 인터페이스를 분리해서 구현하는 것도 가능하기 때문에 새롭게 변경된 이 방식을 사용하는
//            것을 더 권장한다.
//참고: 항상 사용자 정의 리포지토리가 필요한 것은 아니다. 그냥 임의의 리포지토리를 만들어도 된다.
//      예를들어 MemberQueryRepository를 인터페이스가 아닌 클래스로 만들고 스프링 빈으로 등록해서(@Repository)
//      그냥 직접 사용해도 된다. 물론 이 경우 스프링 데이터 JPA와는 아무런 관계 없이 별도로 동작한다.
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
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
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
/*
                .selectFrom(member)
*/
                .leftJoin(member.team, team)
                // select Projection 이 달라져도 조건을 재사용 가능하다.
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
/*
                        ageBetween(condition.getAgeGoe(), condition.getAgeLoe())
*/
                )
                .fetch();
    }

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
/*
                .selectFrom(member)
*/
                .leftJoin(member.team, team)
                // select Projection 이 달라져도 조건을 재사용 가능하다.
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
/*
                        ageBetween(condition.getAgeGoe(), condition.getAgeLoe())
*/
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();// 향후 미지원

        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

/*
        // 향후 미지원 (fetchCount)
        long total = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetchCount();

        return new PageImpl<>(content, pageable, total);
*/

        // PageableExecutionUtils.getPage()로 CountQuery최적화
        // 스프링 데이터 라이브러리가 제공
        // count 쿼리가 생략 가능한 경우 생략해서 처리
        //  - 페이지 시작이면서 컨텐츠 사이즈가 페이지 사이즈보다 작을 때
        //  - 마지막 페이지 일 때 (offset + 컨텐츠 사이즈를 더해서 전체 사이즈 구함)
        JPAQuery<Long> countQuery = queryFactory
                .select(member.count())  //select count(member.id)
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );
        return PageableExecutionUtils.getPage(content, pageable,
                () -> countQuery.fetchOne());

    }

    // 반환타입을 `Predicate` 보다 `BooleanExpression` 으로 하는 것이 향후 확장성이 좋다. composition이 가능하다.(결합) (`QuerydslBasicTest`의 `allEq` 참고)

    private BooleanExpression usernameEq(String usernameCond) {
//        return usernameCond != null ? member.username.eq(usernameCond) : null;
        return hasText(usernameCond) ? member.username.eq(usernameCond) : null;  // 공백문자가 넘어오는 것까지 체크
    }
    private BooleanExpression teamNameEq(String teamNameCond) {
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