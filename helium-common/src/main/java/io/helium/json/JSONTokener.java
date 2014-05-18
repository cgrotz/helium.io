/*
 * Copyright 2012 The Helium Project
 *
 * The Helium Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.helium.json;

import java.io.*;

/**
 * A JSONTokener takes a source string and extracts characters and tokens from
 * it. It is used by the Node and JSONArray constructors to parse JSON source
 * strings.
 *
 * @author JSON.org
 * @version 2012-02-16
 */
public class JSONTokener {

    private long character;
    private boolean eof;
    private long index;
    private long line;
    private char previous;
    private Reader reader;
    private boolean usePrevious;

    /**
     * Construct a JSONTokener from a Reader.
     *
     * @param reader A reader.
     */
    public JSONTokener(Reader reader) {
        this.reader = reader.markSupported() ? reader : new BufferedReader(reader);
        this.eof = false;
        this.usePrevious = false;
        this.previous = 0;
        this.index = 0;
        this.character = 1;
        this.line = 1;
    }

    /**
     * Construct a JSONTokener from an InputStream.
     */
    public JSONTokener(InputStream inputStream) {
        this(new InputStreamReader(inputStream));
    }

    /**
     * Construct a JSONTokener from a string.
     *
     * @param s A source string.
     */
    public JSONTokener(String s) {
        this(new StringReader(s));
    }

    /**
     * Get the hex value of a character (base16).
     *
     * @param c A character between '0' and '9' or between 'A' and 'F' or
     *          between 'a' and 'f'.
     * @return An int between 0 and 15, or -1 if c was not a hex digit.
     */
    public static int dehexchar(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'F') {
            return c - ('A' - 10);
        }
        if (c >= 'a' && c <= 'f') {
            return c - ('a' - 10);
        }
        return -1;
    }

    /**
     * Back up one character. This provides a sort of lookahead capability, so
     * that you can test for a digit or letter before attempting to parse the
     * next number or identifier.
     */
    public void back() {
        if (this.usePrevious || this.index <= 0) {
            throw new RuntimeException("Stepping back two steps is not supported");
        }
        this.index -= 1;
        this.character -= 1;
        this.usePrevious = true;
        this.eof = false;
    }

    public boolean end() {
        return this.eof && !this.usePrevious;
    }

    /**
     * Determine if the source string still contains characters that next() can
     * consume.
     *
     * @return true if not yet at the end of the source.
     */
    public boolean more() {
        this.next();
        if (this.end()) {
            return false;
        }
        this.back();
        return true;
    }

    /**
     * Get the next character in the source string.
     *
     * @return The next character, or 0 if past the end of the source string.
     */
    public char next() {
        int c;
        if (this.usePrevious) {
            this.usePrevious = false;
            c = this.previous;
        } else {
            try {
                c = this.reader.read();
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }

            if (c <= 0) { // End of stream
                this.eof = true;
                c = 0;
            }
        }
        this.index += 1;
        if (this.previous == '\r') {
            this.line += 1;
            this.character = c == '\n' ? 0 : 1;
        } else if (c == '\n') {
            this.line += 1;
            this.character = 0;
        } else {
            this.character += 1;
        }
        this.previous = (char) c;
        return this.previous;
    }

    /**
     * Consume the next character, and check that it matches a specified
     * character.
     *
     * @param c The character to match.
     * @return The character.
     * @throws RuntimeException if the character does not match.
     */
    public char next(char c) {
        char n = this.next();
        if (n != c) {
            throw this.syntaxError("Expected '" + c + "' and instead saw '" + n + "'");
        }
        return n;
    }

    /**
     * Get the next n characters.
     *
     * @param n The number of characters to take.
     * @return A string of n characters.
     * @throws RuntimeException Substring bounds error if there are not n characters
     *                          remaining in the source string.
     */
    public String next(int n) {
        if (n == 0) {
            return "";
        }

        char[] chars = new char[n];
        int pos = 0;

        while (pos < n) {
            chars[pos] = this.next();
            if (this.end()) {
                throw this.syntaxError("Substring bounds error");
            }
            pos += 1;
        }
        return new String(chars);
    }

    /**
     * Get the next char in the string, skipping whitespace.
     *
     * @return A character, or 0 if there are no more characters.
     * @throws RuntimeException
     */
    public char nextClean() {
        for (; ; ) {
            char c = this.next();
            if (c == 0 || c > ' ') {
                return c;
            }
        }
    }

    /**
     * Return the characters up to the next close quote character. Backslash
     * processing is done. The formal JSON format does not allow strings in
     * single quotes, but an implementation is allowed to accept them.
     *
     * @param quote The quoting character, either <code>"</code>
     *              &nbsp;<small>(double quote)</small> or <code>'</code>
     *              &nbsp;<small>(single quote)</small>.
     * @return A String.
     * @throws RuntimeException Unterminated string.
     */
    public String nextString(char quote) {
        char c;
        StringBuffer sb = new StringBuffer();
        for (; ; ) {
            c = this.next();
            switch (c) {
                case 0:
                case '\n':
                case '\r':
                    throw this.syntaxError("Unterminated string");
                case '\\':
                    c = this.next();
                    switch (c) {
                        case 'b':
                            sb.append('\b');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 'u':
                            sb.append((char) Integer.parseInt(this.next(4), 16));
                            break;
                        case '"':
                        case '\'':
                        case '\\':
                        case '/':
                            sb.append(c);
                            break;
                        default:
                            throw this.syntaxError("Illegal escape.");
                    }
                    break;
                default:
                    if (c == quote) {
                        return sb.toString();
                    }
                    sb.append(c);
            }
        }
    }

    /**
     * Get the text up but not including the specified character or the end of
     * line, whichever comes first.
     *
     * @param delimiter A delimiter character.
     * @return A string.
     */
    public String nextTo(char delimiter) {
        StringBuffer sb = new StringBuffer();
        for (; ; ) {
            char c = this.next();
            if (c == delimiter || c == 0 || c == '\n' || c == '\r') {
                if (c != 0) {
                    this.back();
                }
                return sb.toString().trim();
            }
            sb.append(c);
        }
    }

    /**
     * Get the text up but not including one of the specified delimiter
     * characters or the end of line, whichever comes first.
     *
     * @param delimiters A set of delimiter characters.
     * @return A string, trimmed.
     */
    public String nextTo(String delimiters) {
        char c;
        StringBuffer sb = new StringBuffer();
        for (; ; ) {
            c = this.next();
            if (delimiters.indexOf(c) >= 0 || c == 0 || c == '\n' || c == '\r') {
                if (c != 0) {
                    this.back();
                }
                return sb.toString().trim();
            }
            sb.append(c);
        }
    }

    /**
     * Get the next value. The value can be a Boolean, Double, Integer,
     * JSONArray, Node, Long, or String, or the Node.NULL object.
     *
     * @return An object.
     * @throws RuntimeException If syntax error.
     */
    public Object nextValue() {
        char c = this.nextClean();
        String string;

        switch (c) {
            case '"':
            case '\'':
                return this.nextString(c);
            case '{':
                this.back();
                return new Node(this);
        }

		/*
         * Handle unquoted text. This could be the values true, false, or null,
		 * or it can be a number. An implementation (such as this one) is
		 * allowed to also accept non-standard forms.
		 * 
		 * Accumulate characters until we reach the end of the text or a
		 * formatting character.
		 */

        StringBuffer sb = new StringBuffer();
        while (c >= ' ' && ",:]}/\\\"[{;=#".indexOf(c) < 0) {
            sb.append(c);
            c = this.next();
        }
        this.back();

        string = sb.toString().trim();
        if ("".equals(string)) {
            throw this.syntaxError("Missing value");
        }
        return Node.stringToValue(string);
    }

    /**
     * Skip characters until the next character is the requested character. If
     * the requested character is not found, no characters are skipped.
     *
     * @param to A character to skip to.
     * @return The requested character, or zero if the requested character is
     * not found.
     */
    public char skipTo(char to) {
        char c;
        try {
            long startIndex = this.index;
            long startCharacter = this.character;
            long startLine = this.line;
            this.reader.mark(1000000);
            do {
                c = this.next();
                if (c == 0) {
                    this.reader.reset();
                    this.index = startIndex;
                    this.character = startCharacter;
                    this.line = startLine;
                    return c;
                }
            } while (c != to);
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }

        this.back();
        return c;
    }

    /**
     * Make a RuntimeException to signal a syntax error.
     *
     * @param message The error message.
     * @return A RuntimeException object, suitable for throwing
     */
    public RuntimeException syntaxError(String message) {
        return new RuntimeException(message + this.toString());
    }

    /**
     * Make a printable string of this JSONTokener.
     *
     * @return " at {index} [character {character} line {line}]"
     */
    @Override
    public String toString() {
        return " at " + this.index + " [character " + this.character + " line " + this.line + "]";
    }
}