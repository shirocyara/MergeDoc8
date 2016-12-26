/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.ui;

import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mergedoc.MergeDocException;
import mergedoc.core.MergeManager;
import mergedoc.core.Preference;
import mergedoc.core.WorkingState;
import mergedoc.xml.ConfigManager;
import mergedoc.xml.Persister;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 各パネルを配置するフレームです。
 * @author Shinji Kashihara
 */
public class MergeDocFrame extends JFrame {

    /** ロガー */
    private static final Log log = LogFactory.getLog(MergeDocFrame.class);

    /** 設定画面のタイトル */
    private static final String SETTING_TITLE = "設定";
    
    /** メインパネル */
    private JPanel mainPanel = new JPanel();

    /** 設定パネル */
    private PreferencePanel preferencePanel;

    /** 進捗パネル */
    private ProgressPanel progressPanel = new ProgressPanel();

    /** ボタンバー */
    private ButtonBar buttonBar = new ButtonBar();

    /** メッセージダイアログ */
    private MessageDialog dialog = new MessageDialog(this);

    /** マージマネージャ */
    private MergeManager mergeManager;

    /** マージ Executor */
    private ExecutorService mergeExecutor = Executors.newSingleThreadExecutor();

    /**
     * コンストラクタです。
     */
    public MergeDocFrame() {
        try {
            initComponent();
        } catch (Exception e) {
            dialog.showErrorMessage(e.getMessage());
            System.exit(0);
        }
    }
    
    /**
     * コンポーネントを初期化します。
     * @throws MergeDocException 初期化に失敗した場合
     */
    private void initComponent() throws MergeDocException {

        // 設定パネルとマージマネージャを生成
        preferencePanel = new PreferencePanel();
        mergeManager = new MergeManager();

        // メインパネルに設定パネルを追加
        mainPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
        mainPanel.add(preferencePanel);

        // 外側パネルにメインパネルとボタンバーを追加
        JPanel outerPanel = new JPanel();
        outerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));
        outerPanel.add(mainPanel);
        outerPanel.add(ComponentFactory.createSpacer(0, 7));
        outerPanel.add(buttonBar);
        getContentPane().add(outerPanel);

        // リスナー初期化
        initListener();
        buttonBar.setState(buttonBar.INIT_STATE);

        // アイコン設定
        ConfigManager config = ConfigManager.getInstance();
        String iconPath = config.getFile("icon.png").toString();
        Image icon = Toolkit.getDefaultToolkit().createImage(iconPath);
        setIconImage(icon);

        // フレーム設定
        setTitle(SETTING_TITLE);
        Persister psst = Persister.getInstance();
        setLocation(
            psst.getInt(Persister.WINDOW_X, 0),
            psst.getInt(Persister.WINDOW_Y, 0)
        );
        setSize(
            psst.getInt(Persister.WINDOW_WIDTH,  700),
            psst.getInt(Persister.WINDOW_HEIGHT, 570)
        );
        int state = psst.getInt(Persister.WINDOW_STATE, NORMAL);
        if ((state & Frame.ICONIFIED) != ICONIFIED) {
            setExtendedState(state);
        }
        setVisible(true);
    }
    
    /**
     * イベントリスナーを初期化します。
     */
    private void initListener() {

        // 実行ボタンのリスナー
        buttonBar.setRunListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mergeExecutor.submit(new Runnable(){
                    public void run() {execute();}
                });
            }
        });

        // キャンセルボタンのリスナー
        buttonBar.setCancelListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                buttonBar.setEnabled(false);
                mergeManager.getWorkingState().cancel();
            }
        });

        // 戻るボタンのリスナー
        buttonBar.setBackListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                back();
            }
        });

        // 終了ボタンのリスナー
        buttonBar.setEndListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                end();
            }
        });

        // 進捗表示のリスナー
        mergeManager.setChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                WorkingState state = mergeManager.getWorkingState();
                progressPanel.addListText(state.getWorkingText());
                setTitle(progressPanel.getBarText());
            }
        });
        
        // ウィンドウリサイズ後のリスナー
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                persistent();
            }
        });
        
        // ウィンドウクローズ時のリスナー
        addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e) {
                end();
            }
        });
    }
    
    /**
     * マージを実行します。
     */
    public void execute() {

        buttonBar.setEnabled(false);
        Preference pref = preferencePanel.getPreference();
        mergeManager.setPreference(pref);
        
        // ユーザに実行確認
        if (isCancelByConfirm(pref)) {
            buttonBar.setEnabled(true);
            return;
        }

        // 進捗パネル表示
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                mainPanel.remove(preferencePanel);
                progressPanel.init();
                mainPanel.add(progressPanel);
                progressPanel.repaint();
            }
        });
        buttonBar.setState(buttonBar.WORKING_STATE);

        // 設定値の検証
        try {
            mergeManager.validate();
            progressPanel.setMaximum(mergeManager.entrySize());
        } catch (MergeDocException e) {
            dialog.showErrorMessage(e.getMessage());
            back();
            return;
        } catch (Exception e) {
        	String msg = "設定値の検証でエラーが発生しました。";
            log.error(msg, e);
            dialog.showErrorMessage(msg + "\n" + e);
            back();
            return;
        }

        // マージ
        try {
            mergeManager.execute();
        } catch (Exception e) {
            setTitle("エラーが発生しました。");
        	String msg = "マージ処理でエラーが発生しました。";
            log.error(msg, e);
            dialog.showErrorMessage(msg + "\n" + e);
            buttonBar.setState(buttonBar.FINISH_STATE);
            progressPanel.setBarText("異常終了しました。");
            return;
        }
        
        // 処理結果の表示
        WorkingState state = mergeManager.getWorkingState();
        String message = null;
        if (state.isCanceled()) {
            buttonBar.setState(buttonBar.CANCEL_STATE);
            message = "キャンセルしました。";
        } else {
            buttonBar.setState(buttonBar.FINISH_STATE);
            message = "完了しました。 " + state.getWorkTime() + "秒";
        }
        progressPanel.setBarText(message);
        setTitle(message);
    }


    /**
     * ユーザに確認メッセージを表示し実行確認を行います。
     * @param pref マージ設定
     * @return キャンセルされた場合は true
     */
    private boolean isCancelByConfirm(Preference pref) {
        
        if (pref.getDocDirectory().getPath().length() == 0) {
            int result = dialog.showConfirmMessage(
                "Javadoc API 参照ディレクトリが設定されていないためマージは行われません。\n" +
                "文字コード変換および詳細設定は適用されます。実行しますか？");
            if (result != JOptionPane.YES_OPTION) {
                return true;
            }
        }
        if (pref.getOutputArchive().exists()) {
            int result = dialog.showConfirmMessage(
                "出力ソースアーカイブファイルは既に存在します。\n" +
                "上書きしますか？");
            if (result != JOptionPane.YES_OPTION) {
                return true;
            }
        }
        return false;
    }

    /**
     * 進捗パネルを消去して設定パネルを表示します。<br>
     * このメソッドはスレッドに対して安全です。
     */
    private void back() {
        buttonBar.setState(buttonBar.INIT_STATE);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                mainPanel.remove(progressPanel);
                mainPanel.add(preferencePanel);
                mainPanel.repaint();
            }
        });
        setTitle(SETTING_TITLE);
    }

    /**
     * アプリケーションを終了します。
     * ウィンドウ状態や設定内容は設定ファイルに保存されます。
     */
    private void end() {
        persistent();
        try {
            preferencePanel.persistent();
            Persister.getInstance().store();
        } catch (MergeDocException e) {
            dialog.showErrorMessage(e.getMessage());
        }    
        System.exit(0);
    }
    
    /**
     * ウィンドウ状態を Persister にセットします。
     * ウィンドウリサイズ時やアプリケーション終了時に呼び出されます。
     */
    private void persistent() {
        try {
            Persister psst = Persister.getInstance();
            int state = getExtendedState();
            psst.setInt(Persister.WINDOW_STATE, state);
            if (state == Frame.NORMAL) {
                psst.setInt(Persister.WINDOW_WIDTH, (int) getSize().getWidth());
                psst.setInt(Persister.WINDOW_HEIGHT, (int) getSize().getHeight());
                psst.setInt(Persister.WINDOW_X, (int) getLocation().getX());
                psst.setInt(Persister.WINDOW_Y, (int) getLocation().getY());
            }
        } catch (MergeDocException e) {
            dialog.showErrorMessage(e.getMessage());
        }    
    }

    /**
     * このフレームのタイトルを、指定された文字列に設定します。
     * このメソッドはスレッドに対して安全です。
     * @param   title フレームのタイトル
     * @see     Frame#setTitle(String)
     */
    public void setTitle(final String title) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	MergeDocFrame.super.setTitle("MergeDoc - " + title);
            }
        });
    }
}
