package com.himedia.board.global.interceptor;

import com.himedia.board.global.session.SessionConst;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 로그인 체크는 이 인터셉터 한 곳에서만 한다. (docs/05-api.txt "로그인 체크" 표)
 */
public class LoginCheckInterceptor implements HandlerInterceptor {

	private static final Pattern POST_DETAIL = Pattern.compile("^/posts/\\d+$");

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws IOException {
		String contextPath = request.getContextPath();
		String path = request.getRequestURI().substring(contextPath.length());
		boolean isGet = "GET".equalsIgnoreCase(request.getMethod());

		if (isGet && isPublicGetPath(path)) {
			return true;
		}

		HttpSession session = request.getSession(false);
		if (session != null && session.getAttribute(SessionConst.LOGIN_MEMBER) != null) {
			return true;
		}

		if (isGet) {
			String query = request.getQueryString();
			String original = query == null ? path : path + "?" + query;
			response.sendRedirect(contextPath + "/login?redirectURL="
					+ URLEncoder.encode(original, StandardCharsets.UTF_8));
		} else {
			response.sendRedirect(contextPath + "/login");
		}
		return false;
	}

	private boolean isPublicGetPath(String path) {
		return path.equals("/") || path.equals("/posts") || POST_DETAIL.matcher(path).matches();
	}
}
