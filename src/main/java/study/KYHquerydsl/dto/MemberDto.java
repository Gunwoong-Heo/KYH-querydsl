package study.KYHquerydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {

    private String username;
    private int age;

    @QueryProjection  // Dto도 Q파일로 생성(컴파일 해야함)
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}