/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc;

/**
 * MergeDoc アプリケーションの例外です。
 * @author Shinji Kashihara
 */
public class MergeDocException extends Exception {

    /**
     * コンストラクタです。 
     */
    public MergeDocException() {
    }

    /**
     * コンストラクタです。 
     * @param message メッセージ
     */
    public MergeDocException(String message) {
        super(message);
    }

    /**
     * コンストラクタです。 
     * @param cause 原因
     */
    public MergeDocException(Throwable cause) {
        super(cause);
    }

    /**
     * コンストラクタです。 
     * @param message メッセージ
     * @param cause 原因
     */
    public MergeDocException(String message, Throwable cause) {
        super(message, cause);
    }
}
