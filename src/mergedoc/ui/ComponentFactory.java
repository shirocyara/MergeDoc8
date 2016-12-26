/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.ui;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.geom.Dimension2D;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

/**
 * コンポーネントファクトリです。
 * @author Shinji Kashihara
 */
public class ComponentFactory {

    /** このアプリケーションがサポートするコンポーネント最大サイズ（不変） */
    private static final Dimension maxDimension = new Dimension(3200, 2400) {
        public void setSize(Dimension d) {}
        public void setSize(double width, double height) {}
        public void setSize(int width, int height) {}
        public void setSize(Dimension2D d) {}
    };

    /**
     * コンストラクタです。生成不可。
     */
    private ComponentFactory() {
    }

    /**
     * このアプリケーションがサポートするコンポーネント最大サイズを作成します。
     * この Dimension は通常の Dimension と異なりサイズを変更するメソッドを
     * 呼び出してもサイズが変わらない不変クラスです。
     * @return コンポーネントの最大サイズ
     */
    public static Dimension createMaxDimension() {
        return maxDimension;
    }
    
    /**
     * サイズ固定のスペーサを作成します。
     * @param width 横サイズ
     * @param hight 縦サイズ
     * @return スペーサ
     */
    public static JComponent createSpacer(int width, int hight) {
        JLabel label = new JLabel();
        ensureSize(label, width, hight);
        return label;
    }

    /**
     * 指定したコンポーネントに固定のサイズを設定します。
     * @param compo コンポーネント
     * @param width 横サイズ
     * @param hight 縦サイズ
     */
    public static void ensureSize(JComponent compo, int width, int hight) {
        Dimension dim = new Dimension(width, hight);
        compo.setPreferredSize(dim);
        compo.setMinimumSize(dim);
        compo.setMaximumSize(dim);
    }
    
    /**
     * 縦スクロール可能なスクロールペインを作成します。
     * @param compo ビューポートに表示するコンポーネント
     * @return スクロールペイン
     */
    public static JScrollPane createScrollPane(JComponent compo) {
        JScrollPane scrollPane = new JScrollPane(compo);
        setupVerticalScrollPane(scrollPane);
        return scrollPane;
    }
    
    /**
     * スクロールペインに縦スクロール設定します。
     * @param scrollPane スクロールペイン
     */
    public static void setupVerticalScrollPane(JScrollPane scrollPane) {
        scrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    }
    
    /**
     * 指定したコンポーネント配列に設定されているフォントメトリクスから
     * 最も長いテキストの幅に各コンポーネントの幅を合わせます。
     * @param compos コンポーネント配列
     */
    public static void ensureMaxFontWidth(JComponent[] compos) {

        int maxWidth = 0;
        
        for (JComponent compo : compos) {
            FontMetrics metrics = compo.getFontMetrics(compo.getFont());
            int width = 0;

            if (compo instanceof JLabel) {
                width = metrics.stringWidth(((JLabel) compo).getText());
            } else if (compo instanceof JComboBox) {
                ComboBoxModel model = ((JComboBox) compo).getModel();
                for (int j = 0; j < model.getSize(); j++) {
                    width = metrics.stringWidth(model.getElementAt(j).toString());
                    width += 30; //プルダウン部分を加算
                    if (width > maxWidth ) maxWidth = width;
                }
            } else {
                throw new IllegalArgumentException(
                "引数は JLabel[] または JComboBox[] でなければなりません。");
            }

            if (width > maxWidth ) maxWidth = width;
        }
        
        for (JComponent compo : compos) {
            ComponentFactory.ensureSize(compo, maxWidth, 20);
        }
    }
}
