package com.himedia.board.global.web;

/**
 * 로그인 후 이동할 redirectURL 검증 (오픈 리다이렉트 방지).
 */
public final class RedirectUrls {

	private RedirectUrls() {
	}

	/**
	 * "/"로 시작하고 "//"로 시작하지 않는 내부 경로만 그대로 돌려주고, 아니면 "/"를 돌려준다.
	 * 브라우저가 역슬래시를 슬래시처럼 해석하므로("/\evil.com") 역슬래시가 포함된 경로도 차단한다.
	 */
	public static String safe(String redirectURL) {
		if (redirectURL == null
				|| !redirectURL.startsWith("/")
				|| redirectURL.startsWith("//")
				|| redirectURL.contains("\\")) {
			return "/";
		}
		return redirectURL;
	}
}
