package com.example.newsfeed.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

  //InvalidInputException
  //비밀번호 변경 시 이전 비밀번호와 같은 비밀번호를 사용했을 때 출력하는 오류 메시지
  SAME_PASSWORD("비밀번호는 다른 비밀번호를 사용하여야합니다.", HttpStatus.BAD_REQUEST),
  //비밀번호가 틀렸을 때 출력하는 오류 메시지
  WRONG_PASSWORD("비밀 번호가 틀렸습니다.", HttpStatus.BAD_REQUEST),
  //아이디 비밀번호가 잘못됨
  DIFFERENT_EMAIL_PASSWORD("이메일 혹은 비밀번호가 잘못되었습니다.", HttpStatus.BAD_REQUEST),
  //탈퇴한 이메일로 가입을 시도할 때 출력하는 오류 메시지
  EMAIL_DELETED("삭제된 이메일 입니다.", HttpStatus.BAD_REQUEST),
  //이미 친구인 사용자에게 다시 친구신청을 할 때 출력하는 메시지
  ALREADY_FRIEND("이미 친구 상태입니다.", HttpStatus.BAD_REQUEST),
  //이미 친구신청을 보낸 사용자에게 다시 친구신청을 할 때 출력하는 메시지
  ALREADY_SEND("이미 친구 신청을 보냈습니다.", HttpStatus.BAD_REQUEST),
  //거절한 사용자에게 친구신청을 보냈을 때 다시 친구신청을 할 때 출력하는 메시지
  REJECT_SEND("친구신청을 거절한 사용자입니다.", HttpStatus.BAD_REQUEST),
  //자기자신에게 친구요청을 보낸 경우에 출력하는 메시지
  SELF_FRIEND("자기자신에게 친구요청을 보낼 수 없습니다.", HttpStatus.BAD_REQUEST),

  // NotFoundException

  NOT_FOUND_NEWSFEED("뉴스피드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

  NOT_FOUND_NEWSFEED_LIKE("좋아요를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

  NOT_FOUND_MEMBER("멤버를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

  NOT_FOUND_EMAIL("이메일을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

  NOT_FOUND_COMMENT("댓글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

  NOT_FOUND_FRIEND_REQUEST("친구신청을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

  // InternalServerException
  //세션이 만료되었을 때 출력하는 오류 메시지
  SESSION_TIMEOUT("세션이 만료되었습니다.", HttpStatus.SERVICE_UNAVAILABLE),
  //잘못된 요청
  WRONG_REQUEST("지원하지 않는 요청입니다.", HttpStatus.SERVICE_UNAVAILABLE),

  // DuplicatedException
  //이미 좋아요를 누른 게시글에 좋아요를 누른 경우
  ALREADY_LIKED("이미 좋아요를 누른 게시물입니다.", HttpStatus.BAD_REQUEST),
  //중복된 이메일로 가일 할 때 출력하는 오류 메시지
  EMAIL_EXIST("중복된 아이디 입니다.", HttpStatus.BAD_REQUEST),

  // NoAuthorizedException
  //로그인이 안 되었을 떄 출력하는 오류 메시지
  NO_SESSION("로그인이 필요합니다.", HttpStatus.UNAUTHORIZED),
  //이미 로그인이 되어있을 때 출력하는 오류 메시지
  ALREADY_LOGIN("이미 로그인이 되어있습니다.", HttpStatus.UNAUTHORIZED),
  //권한이 없는 사용자가 수정, 삭제를 하려고 할 때
  NO_AUTHOR_CHANGE("수정, 삭제는 작성자만 할 수 있습니다.", HttpStatus.UNAUTHORIZED),
  //프로필 본인 조회
  NO_AUTHOR_PROFILE("프로필은 본인만 조회할 수 있습니다.", HttpStatus.UNAUTHORIZED),

  NO_SELF_LIKE("자신이 작성한 게시글과 댓글에 좋아요를 달 수 없습니다.", HttpStatus.UNAUTHORIZED),

  NO_AUTHOR_APPROVE("이 친구 요청을 승인하거나 거절할 권한이 없습니다.", HttpStatus.UNAUTHORIZED);

  private final String message;
  private final HttpStatus httpStatus;

  ErrorCode(String message, HttpStatus httpStatus) {
    this.message = message;
    this.httpStatus = httpStatus;
  }
}
