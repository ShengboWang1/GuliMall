package com.shengbo.gulimall.order.interceptor;

import com.shengbo.common.constant.AuthServerConstant;
import com.shengbo.common.vo.MemberResponseVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginUserInterceptor implements HandlerInterceptor {
    //
    public static ThreadLocal<MemberResponseVo> loginUser = new ThreadLocal<>();
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String requestURI = request.getRequestURI();
        //这种情况下就放行
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        boolean match = antPathMatcher.match("/order/order/status/**", requestURI);
        boolean match1 = antPathMatcher.match("/payed/notify", requestURI);
        if(match || match1){
            return true;
        }


        MemberResponseVo attribute = (MemberResponseVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
        if(attribute!=null){
            loginUser.set(attribute);
            return true;
        }else {
            //没登录去登录
            request.getSession().setAttribute("msg","请先登录哦");
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }


    }
}
