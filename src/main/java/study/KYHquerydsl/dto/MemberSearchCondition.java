package study.KYHquerydsl.dto;

import lombok.Data;

@Data
public class MemberSearchCondition {
    // 회원명, 팀명, 나이(ageGoe, ageLoe)

    private String username;
    private String teamName;
    private Integer ageGoe;  // 값이 null 일수도 있기 때문에 Integer 사용  (  (condition.getAgeGoe() != null) <- 이런 방식으로 검증  )
    private Integer ageLoe;

}