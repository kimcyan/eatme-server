package com.example.eatmeserver.security.jwt;

import com.example.eatmeserver.common.util.code.ErrorCode;
import com.example.eatmeserver.common.util.exception.BusinessExceptionHandler;
import com.example.eatmeserver.security.AuthConstants;
import com.example.eatmeserver.common.util.login_utils.TokenUtils;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Component
@Slf4j
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws IOException, ServletException {

        // 1. 토큰이 필요하지 않은 API URL에 대해서 배열로 구성합니다.
        List<String> list = Arrays.asList(
                "/api/login/login",
                "/api/login/findId",
                "/api/login/findPw",
                "/api/login/resetPw",
                "/api/join/insert",
                "/api/login/generateToken",
                "/api/wishList/query",
                "/api/wishList/insert",
                "/api/wishList/delete",
                "/api/purchaseHistory/query",
                "/api/purchaseHistory/query/detail",
                "/api/itemReg/query",
                "/api/itemReg/insert",
                "/api/itemReg/update",
                "/api/itemReg/delete",
                "/api/dashboard/selling",
                "/api/dashboard/qnaList",
                "/api/dashboard/qnaDetail",
                "/api/dashboard/updateQnaAns",
                "/api/basket/query",
                "/api/basket/insert",
                "/api/basket/delete",
                "/api/admin/qna/query",
                "/api/admin/qna/detail",
                "/api/admin/qna/update",
                "/api/admin/report/query",
                "/api/admin/report/detail",
                "/api/admin/notice/query",
                "/api/admin/notice/detail",
                "/api/admin/notice/insert",
                "/api/admin/notice/update",
                "/api/goodsReg/insert",
                "/api/goodsReg/update",
                "/api/goodsReg/delete",
                "/api/goodsReg/query",
                "/api/goodsMgm/query",
                "/api/main/query/item",
                "/api/main/query/corp",
                "/api/qna/query",
                "/api/sellerProfile/query",
                "/api/sellerReg/insert",
                "/api/sellerProfile/update",
                "/api/sellerNotice/query",
                "/api/mypage/query",
                "/api/mypage/changeUser",
                "/api/mypage/exit",
                "/api/ecoStatus/query",
                "/api/ecoStatus/queryMyEco",
                "/api/file/upload",
                "/api/file/getImg",
                "/api/marketInfo/query",
                "/api/login/getCorpCd",
                "/api/file/getImgInfo",
                "/api/join/checkOverlap",
                "/api/itemReg/getSeq"
        );

        // 2. 토큰이 필요하지 않은 API URL의 경우 => 로직 처리 없이 다음 필터로 이동
        if (list.contains(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        // 3. OPTIONS 요청일 경우 => 로직 처리 없이 다음 필터로 이동
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            chain.doFilter(request, response);
            return;
        }

        // [STEP1] Client에서 API를 요청할때 Header를 확인합니다.
        String header = request.getHeader(AuthConstants.AUTH_HEADER);
        logger.debug("[+] header Check: " + header);

        try {
            // [STEP2-1] Header 내에 토큰이 존재하는 경우
            if (header != null && !header.equalsIgnoreCase("")) {

                // [STEP2] Header 내에 토큰을 추출합니다.
                String token = TokenUtils.getTokenFromHeader(header);

                // [STEP3] 추출한 토큰이 유효한지 여부를 체크합니다.
                if (TokenUtils.isValidToken(token)) {

                    // [STEP4] 토큰을 기반으로 사용자 아이디를 반환 받는 메서드
                    String userId = TokenUtils.getUserIdFromToken(token);
                    logger.debug("[+] userId Check: " + userId);

                    // [STEP5] 사용자 아이디가 존재하는지 여부 체크
                    if (userId != null && !userId.equalsIgnoreCase("")) {
                        chain.doFilter(request, response);
                    } else {
                        throw new BusinessExceptionHandler("TOKEN isn't userId", ErrorCode.BUSINESS_EXCEPTION_ERROR);
                    }
                    // 토큰이 유효하지 않은 경우
                } else {
                    throw new BusinessExceptionHandler("TOKEN is invalid", ErrorCode.BUSINESS_EXCEPTION_ERROR);
                }
            }
            // [STEP2-1] 토큰이 존재하지 않는 경우
            else {
                throw new BusinessExceptionHandler("Token is null", ErrorCode.BUSINESS_EXCEPTION_ERROR);
            }
        } catch (Exception e) {
            // Token 내에 Exception이 발생 하였을 경우 => 클라이언트에 응답값을 반환하고 종료합니다.
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            PrintWriter printWriter = response.getWriter();
            JSONObject jsonObject = jsonResponseWrapper(e);
            printWriter.print(jsonObject);
            printWriter.flush();
            printWriter.close();
        }
    }

    /**
     * 토큰 관련 Exception 발생 시 예외 응답값 구성
     *
     * @param e Exception
     * @return JSONObject
     */
    private JSONObject jsonResponseWrapper(Exception e) {

        String resultMsg = "";
        // JWT 토큰 만료
        if (e instanceof ExpiredJwtException) {
            resultMsg = "TOKEN Expired";
        }
        // JWT 허용된 토큰이 아님
        else if (e instanceof SignatureException) {
            resultMsg = "TOKEN SignatureException Login";
        }
        // JWT 토큰내에서 오류 발생 시
        else if (e instanceof JwtException) {
            resultMsg = "TOKEN Parsing JwtException";
        }
        // 이외 JTW 토큰내에서 오류 발생
        else {
            resultMsg = "OTHER TOKEN ERROR";
        }

        HashMap<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("status", 401);
        jsonMap.put("code", "9999");
        jsonMap.put("message", resultMsg);
        jsonMap.put("reason", e.getMessage());
        JSONObject jsonObject = new JSONObject(jsonMap);
        logger.error(resultMsg, e);
        return jsonObject;
    }
}
