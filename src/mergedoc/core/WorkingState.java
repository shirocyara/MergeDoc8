/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.core;

import javax.swing.event.ChangeListener;

/**
 * 処理状態を保持するクラスです。
 * @author Shinji Kashihara
 */
public class WorkingState {

    /** 状態監視用のリスナ */
    private ChangeListener changeListener;

    /** 処理対象文字列 */
    private String workingText;

    /** 処理対象文字列変更回数 */
    private int changedCount;

    /** キャンセル */
    private boolean canceled;

    /** 処理に要した時間（秒） */
    private long workTime;

    /**
     * コンストラクタです。 
     */
    public WorkingState() {
    }

    /**
     * 状態を初期化します。
     */
    void initialize() {
        canceled = false;
    }

    /**
     * 進捗監視用のリスナをセットします。
     * @param changeListener 進捗監視用のリスナ
     */
    void setChangeListener(ChangeListener changeListener) {
        this.changeListener = changeListener;
    }

    /**
     * 処理対象を表わす文字列を設定します。
     * @param text 処理対象を表わす文字列
     */
    void changeWorkingText(String text) {
        workingText = text;
        changedCount++;
        changeListener.stateChanged(null);
    }

    /**
     * キャンセル状態にします。
     */
    public void cancel() {
        canceled = true;
    }

    /**
     * 処理対象を表わす文字列を取得します。
     * @return 処理対象を表わす文字列
     */
    public String getWorkingText() {
        return workingText;
    }

    /**
     * キャンセル済みか判定します。
     * @return キャンセル済みの場合は true
     */
    public boolean isCanceled() {
        return canceled;
    }
    
    /**
     * 処理対象文字列変更回数を取得します．
     * @return 処理対象文字列変更回数
     */
    public int getChangedCount() {
        return changedCount;
    }
    
    /**
     * 処理に要した時間をセットします．
     * @param workTime 処理に要した時間（秒）
     */
    public void setWorkTime(long workTime) {
        this.workTime = workTime;
    }
    
    /**
     * 処理に要した時間を取得します．
     * @return 処理に要した時間（秒）
     */
    public long getWorkTime() {
        return workTime;
    }
}
