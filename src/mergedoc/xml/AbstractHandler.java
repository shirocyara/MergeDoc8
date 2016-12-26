/*
 * Copyright (c) 2003- Shinji Kashihara. All rights reserved.
 * This program are made available under the terms of the Common Public License
 * v1.0 which accompanies this distribution, and is available at cpl-v10.html.
 */
package mergedoc.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX1 パーサの置換定義 XML ファイルハンドラの抽象クラスです。
 * @author Shinji Kashihara
 */
abstract public class AbstractHandler extends DefaultHandler {

    /** 現在のエレメント名 */
    private String currentQName;

    /** ルート置換エントリ */
    private ReplaceEntry rootEntry;

    /** カレント置換エントリ */
    private ReplaceEntry currentEntry;

    /** 置換エントリの階層レベル */
    private int level;

    /**
     * 要素の開始通知を受け取ります。
     * @param   uri        前置修飾子にマッピングされた名前空間 URI
     * @param   localName  前置修飾子を含まないローカル名。名前空間処理が
     *                     行われない場合は空文字列
     * @param   qName      前置修飾子を持つ修飾名。修飾名を使用できない場合は空文字列
     * @param   attributes 指定された属性またはデフォルトの属性
     * @throws  SAXException SAX 例外。ほかの例外をラップしている可能性がある
     * @see org.xml.sax.ContentHandler#startElement
     */
    public void startElement(
        String uri,
        String localName,
        String qName,
        Attributes attributes) throws SAXException
    {
        if ("置換エントリ".equals(qName)) {
            
            String target = attributes.getValue("対象");
            if (level == 0) {

                rootEntry = new ReplaceEntry();
                rootEntry.setTarget(target);
                currentEntry = rootEntry;

            } else {
                
                currentEntry = new ReplaceEntry();
                if (target == null) {
                    currentEntry.setTarget(rootEntry.getTarget());
                } else {
                    currentEntry.setTarget(target);
                }
                rootEntry.addChild(currentEntry);
            }
            level++;
        }
        
        currentQName = qName;
    }

    /**
     * 要素の終了通知を受け取ります。
     * @param   uri       前置修飾子にマッピングされた名前空間 URI
     * @param   localName 前置修飾子を含まないローカル名。名前空間処理が
     *                    行われない場合は空文字列
     * @param   qName     前置修飾子を持つ XML 1.0 修飾名。修飾名を使用できない
     *                    場合は空文字列
     * @throws  SAXException SAX 例外。ほかの例外をラップしている可能性がある
     * @see org.xml.sax.ContentHandler#endElement
     */
    public void endElement(String uri, String localName, String qName)
        throws SAXException
    {
        if ("置換エントリ".equals(qName)) {
            level--;
            if (level == 0) {
                handle(rootEntry);
            }
        }
        currentQName = null;
    }

    /**
     * 要素内の文字データの通知を受け取ります。
     * @param   c      文字配列
     * @param   start  文字配列内の開始位置
     * @param   length 文字配列から使用される文字数
     * @throws  SAXException SAX 例外。ほかの例外をラップしている可能性がある
     * @see org.xml.sax.ContentHandler#characters
     */
    public void characters(char[] c, int start, int length) throws SAXException {

        if (currentEntry == null) {
            
        } else if ("説明".equals(currentQName)) {
            String desc = String.valueOf(c, start, length);
            currentEntry.setDescription(desc);
            
        } else if ("前".equals(currentQName)) {
            String before = String.valueOf(c, start, length);
            currentEntry.setBefore(before);
            
        } else if ("後".equals(currentQName)) {
            String after = expandEscape(c, start, length);
            currentEntry.setAfter(after);
        }
    }

    /**
     * 文字配列に含まれるエスケープシーケンスを文字表現に展開します。
     * @param   c      文字配列
     * @param   start  文字配列内の開始位置
     * @param   length 文字配列から使用される文字数
     * @return  展開した文字配列
     */
    private String expandEscape(char[] c, int start, int length) {
            
        StringBuilder sb = new StringBuilder();
        int end = start + length - 1;
            
        for (int i = start; i <= end; i++) {
            if (c[i] == '\\' &&
                (i == start || c[i-1] != '\\') &&
                i != end)
            {
                char ex = ' ';
                switch (c[i+1]) {
                    case 'b': ex='\b'; break;
                    case 'f': ex='\f'; break;
                    case 'n': ex='\n'; break;
                    case 'r': ex='\r'; break;
                    case 't': ex='\t'; break;
                }
                if (ex != ' ') {
                    sb.append(ex);
                    i++;
                    continue;
                }
            }
            sb.append(c[i]);
        }
            
        return sb.toString();        
    }
    
    /**
     * 置換エントリを処理します。
     * @param entry 置換エントリ
     */
    abstract protected void handle(ReplaceEntry entry);
}
