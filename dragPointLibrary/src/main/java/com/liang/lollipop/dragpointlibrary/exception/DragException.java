package com.liang.lollipop.dragpointlibrary.exception;

/**
 * Created by LiuJ on 2017/07/20.
 * 拖拽项目的异常信息
 */
public class DragException extends RuntimeException {
    public DragException() {
    }

    public DragException(String message) {
        super(message);
    }

    public DragException(String message, Throwable cause) {
        super(message, cause);
    }

    public DragException(Throwable cause) {
        super(cause);
    }

}
