/**
 * これは詳細設定を確認するためのプレビューです。<p><a
 * href="http://www.xxx/">リンク</a>
 * <pre>
 *     <code>boolean</code> <b><i>b</i></b> = a &gt; 0 &amp;&amp; b &lt; 0;
 * </pre>
 * <ul>
 * <li>conf/global.xml を編集することで詳細設定をカスタマイズ出来ます。
 * <li>XML のパーサは {@link javax.xml.parsers.SAXParser} です。
 * </ul>
 */
public class HelloMergeDoc extends HelloWorld {

    /**
     * 詳細設定のデフォルトはすべて OFF です。
     * <p>
     * HTML タグや文字実体参照の設定はソースコメントを直接見る場合の
     * 可読性を高めます。ただし装飾タグ以外の削除は <tt>Eclipse</tt>
     * でのホバー表示が崩れる可能性があります。
     * <table>
     * <tr>
     * <td align=center><b>ヘッダカラム1</b></td>
     * <td align=center><b>ヘッダカラム2</b></td>
     * </tr>
     * <tr>
     * <td>カラム1</td>
     * <td>カラム2</td>
     * </tr>
     * </table>
     * 
     * @param   p    パラメータ
     * @throws  IllegalHelloException 挨拶が出来なかった場合
     */
    public void sayHello(String p) {

        // 通常 HTML タグは Javadoc コメント以外のコメントでは
        // 使用されないため置換対象となりません。置換対象は
        // conf/global.xml の「対象」属性で指定出来ます。
        /*
         * ブロックコメント（非 <code>Javadoc</code> コメント）
         * このプレビューのタブ幅設定は 4 です。
         */
        String str = "<code>" + p + "</code>";
        super.sayHello(str); //行コメント<p>
    }
}