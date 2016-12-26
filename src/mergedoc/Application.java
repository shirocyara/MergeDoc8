/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc;

import java.awt.Font;
import java.awt.Toolkit;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;

import mergedoc.ui.MergeDocFrame;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * アプリケーション起動エントリです。
 * @author Shinji Kashihara
 */
public class Application {
	
	/** ロガー */
	private static final Log log = LogFactory.getLog(Application.class);
	
    /**
     * メインです。
     * @param args 起動引数
     */
    public static void main(String[] args) {

        // システム固有の Look & Feel を設定
        try {
            initSystemLookAndFeel();
        } catch (Exception e) {
            log.warn("Look & Feel の設定に失敗しました。", e);
        }

        // フレーム生成
        new MergeDocFrame();
    }

    /**
     * システム固有の Look & Feel を設定します。
     * @throws ClassNotFoundException LookAndFeel クラスが見つからなかった場合
     * @throws InstantiationException クラスの新しいインスタンスを生成できなかった場合
     * @throws IllegalAccessException クラスまたは初期化子にアクセスできない場合
     * @throws UnsupportedLookAndFeelException lnf.isSupportedLookAndFeel() が false の場合
     */
    private static void initSystemLookAndFeel() throws
        ClassNotFoundException,
        InstantiationException,
        IllegalAccessException,
        UnsupportedLookAndFeelException
    {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        Toolkit.getDefaultToolkit().setDynamicLayout(true);

        // Windows の場合のフォント設定
        String osName = System.getProperty("os.name", "");
        if (osName.indexOf("Windows") != -1) {
    
            Object propoFont = new FontUIResource("MS UI Gothic", Font.PLAIN, 12);
            Object fixedFont = new FontUIResource("MS Gothic", Font.PLAIN, 12);
    
            // 一旦すべてのコンポーネントのフォントをプロポーショナルにする。
            // フォントリソースかの判定は値を取得し instanceof FontUIResource が
            // 安全だが、UIDefaults の Lazy Value を生かすため末尾の文字列で判定。
            for (Object keyObj : UIManager.getLookAndFeelDefaults().keySet()) {
                String key = keyObj.toString();
                if (key.endsWith("font") || key.endsWith("Font")) {
                    UIManager.put(key, propoFont);
                }
            }
        
            // 特定コンポーネントのフォントを等幅にする
            UIManager.put("OptionPane.messageFont", fixedFont);
            UIManager.put("TextPane.font", fixedFont);
            UIManager.put("TextArea.font", fixedFont);
        }
    }
}
