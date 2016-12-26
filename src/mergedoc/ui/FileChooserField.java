/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.ui;

import java.awt.Dimension;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * ファイル選択フィールドです。
 * @author Shinji Kashihara
 */
public class FileChooserField extends JPanel {

    /** ロガー */
    private static final Log log = LogFactory.getLog(FileChooserField.class);


    /** ラベル部 */
    private final JLabel label = new JLabel();

    /** テキストフィールド部 */
    private final JTextField textField = new JTextField();

    /** ボタン部 */
    private final JButton button = new JButton();

    /** コンボボックス部 */
    private final JComboBox combo = new JComboBox();
    
    /** ファイル選択ダイアログ */
    private JFileChooser chooser = new JFileChooser();

    /** ファイル選択時のリスナー */
    private ActionListener chooseListener;


    /** エンコーディングデフォルト */
    public static final String ENCODING_DEFAULT = System.getProperty("file.encoding");

    /** エンコーディング自動判別 */
    public static final String ENCODING_AUTO = "JISAutoDetect";


    /** 選択モードインターフェース */
    private static interface SelectionMode {
        void apply(JFileChooser chooser);
    }

    /** ディレクトリ選択モード */
    public static final SelectionMode DIRECTORIES = new SelectionMode() {
        public void apply(JFileChooser chooser) {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.removeChoosableFileFilter(chooser.getFileFilter());
            chooser.setFileFilter(new FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory();
                }
                public String getDescription() {
                    return "ディレクトリのみ";
                }
            });
        }
    };

    /** ZIP ファイル選択モード */
    public static final SelectionMode ZIP_FILES = new SelectionMode() {
        public void apply(JFileChooser chooser) {
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setFileFilter(new FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory() ||
                           f.getName().endsWith(".zip") ||
                           f.getName().endsWith(".jar");
                }
                public String getDescription() {
                    return "*.zip, *.jar";
                }
            });
        }
    };

    /** ZIP, TGZ ファイル選択モード */
    public static final SelectionMode ZIP_TGZ_FILES = new SelectionMode() {
        public void apply(JFileChooser chooser) {
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setFileFilter(new FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory() ||
                           f.getName().endsWith(".zip") ||
                           f.getName().endsWith(".jar") ||
                           f.getName().endsWith(".tgz") ||
                           f.getName().endsWith(".tar.gz");
                }
                public String getDescription() {
                    return "*.zip, *.jar, *.tgz, *.tar.gz";
                }
            });
        }
    };


    /**
     * コンストラクタです。
     */
    public FileChooserField() {

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        // テキストフィールドの設定
        int maxWidth = (int) ComponentFactory.createMaxDimension().getWidth();
        textField.setMaximumSize(new Dimension(maxWidth, 20));
        textField.setTransferHandler(new FileDropHandler());

        // ボタンの設定
        ComponentFactory.ensureSize(button, 20, 18);
        button.setText("...");

        // コンボモデルの作成
        CharsetSortedModel charsetModel = new CharsetSortedModel();
        charsetModel.add("EUC-JP");
        charsetModel.add("EUC-JP-LINUX");
        charsetModel.add("ISO-2022-JP");
        charsetModel.add("MS932");
        charsetModel.add("Shift_JIS");
        charsetModel.add("UTF-16");
        charsetModel.add("UTF-16BE");
        charsetModel.add("UTF-16LE");
        charsetModel.add("UTF-8");
        charsetModel.add(ENCODING_DEFAULT);
        charsetModel.fireAdded();

        // コンポーネントを配置
        add(label);
        add(ComponentFactory.createSpacer(10, 0));
        add(textField);
        add(button);
        add(ComponentFactory.createSpacer(5, 0));
        add(combo);

        // ファイル選択ダイアログの設定
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int result = chooser.showOpenDialog(FileChooserField.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    setFile(chooser.getSelectedFile());
                    if (chooseListener != null) {
                        chooseListener.actionPerformed(e);
                    }
                }
            }
        });
    }

    /**
     * ファイル選択ダイアログの選択モードを設定します。
     * @param mode 選択モード
     */
    public void setSelectionMode(SelectionMode mode) {
        chooser = new JFileChooser();
        mode.apply(chooser);
    }
    
    /**
     * ファイル選択時のリスナーをセットします．
     * @param chooseListener ファイル選択時のリスナー
     */    
    public void setChooseListener(ActionListener chooseListener) {
        this.chooseListener = chooseListener;
    }

    /**
     * ラベルを取得します。
     * @return ラベル
     */
    public JLabel getLabel() {
        return label;
    }

    /**
     * ファイルを設定します。
     * @param file ファイル
     */
    public void setFile(File file) {
        textField.setText(file.getPath());
        chooser.setSelectedFile(file);
    }

    /**
     * ファイルを取得します。
     * @return ファイル
     */
    public File getFile() {
        return new File(textField.getText());
    }
    
    /**
     * コンボボックスを取得します。
     * @return コンボボックス
     */
    public JComboBox getComboBox() {
        return combo;
    }

    /**
     * 文字セット名を昇順で保持するコンボボックスモデルです。<p>
     * 
     * プラットフォームでサポートされない文字セット名が追加された場合は無視します。
     * その判定は String のコンストラクタ {@link String(byte[],String)}
     * を使用します。{@link Charset#isSupported} は JISAutoDetect の判定が
     * 出来ないことや、JDK1.4.1 では ISO-2022-JP の場合 false を返すため
     * 使用されません。<p>
     * 
     * なお、要素を削除は今のところ考慮されておらず、削除した場合の動作は保証しません。
     */
    private class CharsetSortedModel extends DefaultComboBoxModel {

        private final Set<Object> charsetNames;

        private CharsetSortedModel() {
            charsetNames = new TreeSet<Object>();
        }

        private CharsetSortedModel(Set<Object> set) {
            super(set.toArray());
            charsetNames = set;
        }
        
        private boolean isSupported(Object charset) {
            try {
                new String(new byte[0], charset.toString());
            } catch (UnsupportedEncodingException e) {
                return false;
            }
            return true;
        }
        
        private void add(Object charset) {
            if (isSupported(charset)) charsetNames.add(charset);
        }
        
        private void fireAdded() {
            combo.setModel(new CharsetSortedModel(charsetNames));
        }
        
        public void addElement(Object charset) {
            if (isSupported(charset) && !charsetNames.contains(charset)) {
                charsetNames.add(charset);
                fireAdded();
            }
        }

        public void setSelectedItem(Object charset) {
            if (charset != null) {
                if (!charsetNames.contains(charset)) {
                    addElement(charset);
                }
                super.setSelectedItem(charset);
            }
        }
    }

    /**
     * 動作プラットフォームからのテキストフィールドへのファイルのドロップを受け付ける
     * ための転送ハンドラです。ファイルのドロップ以外の転送、例えば文字列のドロップや
     * カット&ペーストなどはテキストフィールドが持つオリジナルの転送ハンドラに委譲します。
     */
    private class FileDropHandler extends TransferHandler {

        TransferHandler originHandler = textField.getTransferHandler();

        public void exportToClipboard(JComponent comp, Clipboard clipboard, int action) {
            originHandler.exportToClipboard(comp, clipboard, action);
        }

        public int getSourceActions(JComponent comp) {
            return originHandler.getSourceActions(comp);
        }

        public boolean canImport(JComponent comp, DataFlavor[] flavors) {
            for (DataFlavor flavor : flavors) {
                if (flavor.isFlavorJavaFileListType()) {
                    return true;
                }
            }
            return originHandler.canImport(comp, flavors);
        }

        public boolean importData(JComponent comp, Transferable t) {
            boolean imported = false;
            if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                try {
                    List files = (List) t.getTransferData(DataFlavor.javaFileListFlavor);
                    File file = (File) files.get(0);
                    setFile(file);
                    textField.repaint();
                    imported = true;
                } catch (UnsupportedFlavorException e) {
                } catch (IOException e) {
                }
            } else {
                imported = originHandler.importData(comp, t);
            }
            if (imported && chooseListener != null && getFile().exists()) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        chooseListener.actionPerformed(null);
                    }
                });
            }
            return imported;
        }
    }
}
