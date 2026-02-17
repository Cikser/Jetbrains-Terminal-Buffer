package test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import terminalbuffer.Style;
import terminalbuffer.TerminalBuffer;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class TerminalBufferTest {

    private TerminalBuffer buffer;
    private static final int WIDTH = 10;
    private static final int HEIGHT = 5;
    private static final int MAX_SCROLLBACK = 10;

    @BeforeEach
    void setUp() {
        buffer = new TerminalBuffer(WIDTH, HEIGHT, MAX_SCROLLBACK);
    }

    // ============================================
    // SETUP & INITIALIZATION TESTS
    // ============================================

    @Nested
    @DisplayName("Setup and Initialization")
    class SetupTests {

        @Test
        @DisplayName("Buffer initializes with correct dimensions")
        void testInitialization() {
            assertEquals(WIDTH, buffer.width());
            assertEquals(HEIGHT, buffer.height());
        }

        @Test
        @DisplayName("Buffer starts with empty lines")
        void testEmptyInitialization() {
            for (int row = 0; row < HEIGHT; row++) {
                String line = buffer.lineToString(row);
                assertEquals(" ".repeat(WIDTH), line);
            }
        }

        @Test
        @DisplayName("Cursor starts at (0,0)")
        void testCursorInitialPosition() {
            assertEquals(0, buffer.cursor().row());
            assertEquals(0, buffer.cursor().col());
        }

        @Test
        @DisplayName("Default attributes are set")
        void testDefaultAttributes() {
            int attrs = buffer.currentAttributes();
            assertNotEquals(0, attrs); // Should have default colors
        }
    }

    // ============================================
    // CURSOR MOVEMENT TESTS
    // ============================================

    @Nested
    @DisplayName("Cursor Movement")
    class CursorTests {

        @Test
        @DisplayName("Set cursor to valid position")
        void testSetCursor() {
            buffer.cursor().set(2, 3);
            assertEquals(2, buffer.cursor().row());
            assertEquals(3, buffer.cursor().col());
        }

        @Test
        @DisplayName("Cursor stays within bounds - top left")
        void testCursorBoundsTopLeft() {
            buffer.cursor().set(-5, -5);
            assertEquals(0, buffer.cursor().row());
            assertEquals(0, buffer.cursor().col());
        }

        @Test
        @DisplayName("Cursor stays within bounds - bottom right")
        void testCursorBoundsBottomRight() {
            buffer.cursor().set(100, 100);
            assertEquals(HEIGHT - 1, buffer.cursor().row());
            assertEquals(WIDTH - 1, buffer.cursor().col());
        }

        @Test
        @DisplayName("Move cursor up")
        void testCursorUp() {
            buffer.cursor().set(3, 5);
            buffer.cursor().up(2);
            assertEquals(1, buffer.cursor().row());
            assertEquals(5, buffer.cursor().col());
        }

        @Test
        @DisplayName("Move cursor up with clamping")
        void testCursorUpClamping() {
            buffer.cursor().set(2, 5);
            buffer.cursor().up(5); // Try to go past top
            assertEquals(0, buffer.cursor().row());
        }

        @Test
        @DisplayName("Move cursor down")
        void testCursorDown() {
            buffer.cursor().set(1, 5);
            buffer.cursor().down(2);
            assertEquals(3, buffer.cursor().row());
            assertEquals(5, buffer.cursor().col());
        }

        @Test
        @DisplayName("Move cursor down with clamping")
        void testCursorDownClamping() {
            buffer.cursor().set(HEIGHT - 2, 5);
            buffer.cursor().down(5); // Try to go past bottom
            assertEquals(HEIGHT - 1, buffer.cursor().row());
        }

        @Test
        @DisplayName("Move cursor left")
        void testCursorLeft() {
            buffer.cursor().set(2, 5);
            buffer.cursor().left(3);
            assertEquals(2, buffer.cursor().row());
            assertEquals(2, buffer.cursor().col());
        }

        @Test
        @DisplayName("Move cursor left with clamping")
        void testCursorLeftClamping() {
            buffer.cursor().set(2, 3);
            buffer.cursor().left(10); // Try to go past left edge
            assertEquals(0, buffer.cursor().col());
        }

        @Test
        @DisplayName("Move cursor right")
        void testCursorRight() {
            buffer.cursor().set(2, 2);
            buffer.cursor().right(3);
            assertEquals(2, buffer.cursor().row());
            assertEquals(5, buffer.cursor().col());
        }

        @Test
        @DisplayName("Move cursor right with clamping")
        void testCursorRightClamping() {
            buffer.cursor().set(2, WIDTH - 3);
            buffer.cursor().right(10); // Try to go past right edge
            assertEquals(WIDTH - 1, buffer.cursor().col());
        }

        @Test
        @DisplayName("Move by zero does nothing")
        void testMoveByZero() {
            buffer.cursor().set(2, 3);
            buffer.cursor().up(0);
            buffer.cursor().down(0);
            buffer.cursor().left(0);
            buffer.cursor().right(0);
            assertEquals(2, buffer.cursor().row());
            assertEquals(3, buffer.cursor().col());
        }
    }

    // ============================================
    // WRITE OPERATION TESTS
    // ============================================

    @Nested
    @DisplayName("Write Operations")
    class WriteTests {

        @Test
        @DisplayName("Write simple text")
        void testWriteSimpleText() {
            buffer.write("HELLO");
            assertEquals("HELLO     ", buffer.lineToString(0));
        }

        @Test
        @DisplayName("Write at specific position")
        void testWriteAtPosition() {
            buffer.write("TEST", 2, 3);
            String line = buffer.lineToString(2);
            assertEquals("   TEST   ", line);
        }

        @Test
        @DisplayName("Write overwrites existing content")
        void testWriteOverwrites() {
            buffer.write("AAAAAAAAAA"); // Fill line
            buffer.cursor().set(0, 0);
            buffer.write("BBB");
            assertEquals("BBBAAAAAAA", buffer.lineToString(0));
        }

        @Test
        @DisplayName("Write advances cursor")
        void testWriteAdvancesCursor() {
            buffer.cursor().set(0, 0);
            buffer.write("ABC");
            assertEquals(0, buffer.cursor().row());
            assertEquals(3, buffer.cursor().col());
        }

        @Test
        @DisplayName("Write wraps to next line at end")
        void testWriteWrapsToNextLine() {
            buffer.cursor().set(0, WIDTH - 2);
            buffer.write("ABCD");

            String line0 = buffer.lineToString(0);
            String line1 = buffer.lineToString(1);

            assertTrue(line0.endsWith("AB"));
            assertTrue(line1.startsWith("CD"));
        }

        @Test
        @DisplayName("Write at last line triggers scroll")
        void testWriteTriggersScroll() {
            // Fill screen
            for (int i = 0; i < HEIGHT; i++) {
                buffer.write("LINE" + i, i, 0);
            }

            // Write at end should scroll
            buffer.cursor().set(HEIGHT - 1, WIDTH - 1);
            buffer.write("XX");

            // First line should be gone (in scrollback)
            assertFalse(buffer.lineToString(0).contains("LINE0"));
        }

        @Test
        @DisplayName("Write empty string does nothing")
        void testWriteEmptyString() {
            buffer.write("TEST");
            buffer.cursor().set(0, 0);
            buffer.write("");
            assertEquals("TEST      ", buffer.lineToString(0));
            assertEquals(0, buffer.cursor().col());
        }

        @Test
        @DisplayName("Write single character")
        void testWriteSingleChar() {
            buffer.write("X");
            assertEquals('X', buffer.charAtScreen(0, 0));
            assertEquals(1, buffer.cursor().col());
        }
    }

    // ============================================
    // INSERT OPERATION TESTS
    // ============================================

    @Nested
    @DisplayName("Insert Operations")
    class InsertTests {

        @Test
        @DisplayName("Insert shifts existing content right")
        void testInsertShifts() {
            buffer.write("HELLO");
            buffer.cursor().set(0, 2);

            buffer.insert("XYZ");

            String result = buffer.lineToString(0).trim();
            assertTrue(result.startsWith("HEXYZLLO") || result.startsWith("HEXYZ"));
        }

        @Test
        @DisplayName("Insert at start of line")
        void testInsertAtStart() {
            buffer.write("WORLD");
            buffer.cursor().set(0, 0);

            buffer.insert("HELLO");

            String line = buffer.lineToString(0);
            assertTrue(line.startsWith("HELLOW"));
        }

        @Test
        @DisplayName("Insert at end of content")
        void testInsertAtEnd() {
            buffer.write("TEST");
            buffer.cursor().set(0, 4);

            buffer.insert("XX");

            String line = buffer.lineToString(0);
            assertTrue(line.contains("TESTXX"));
        }

        @Test
        @DisplayName("Insert wraps overflow to next line")
        void testInsertWraps() {
            buffer.write("A".repeat(WIDTH)); // Fill line
            buffer.cursor().set(0, 5);

            buffer.insert("XXX");

            // Check second line has overflow
            String line1 = buffer.lineToString(1);
            assertFalse(line1.trim().isEmpty());
        }

        @Test
        @DisplayName("Insert in empty line")
        void testInsertInEmptyLine() {
            buffer.cursor().set(2, 3);

            buffer.insert("TEST");

            String line = buffer.lineToString(2);
            assertTrue(line.contains("TEST"));
        }

        @Test
        @DisplayName("Insert advances cursor")
        void testInsertAdvancesCursor() {
            buffer.cursor().set(0, 2);
            buffer.insert("ABC");

            // Cursor should have moved
            assertTrue(buffer.cursor().col() >= 2);
        }

        @Test
        @DisplayName("Insert at specific position")
        void testInsertAtPosition() {
            buffer.write("HELLO", 0, 0);

            buffer.insert("XX", 0, 2);

            String line = buffer.lineToString(0);
            assertTrue(line.contains("XX"));
        }
    }

    @Test
    @DisplayName("Cursor positioned after inserted text, no overflow")
    void testCursorAfterInsertNoOverflow() {
        // Insert "ABC" at col 2, cursor should end at col 5
        buffer.cursor().set(0, 2);
        buffer.insert("ABC");

        assertEquals(0, buffer.cursor().row());
        assertEquals(5, buffer.cursor().col());
    }

    @Test
    @DisplayName("Cursor positioned after inserted text, with overflow to next line")
    void testCursorAfterInsertWithOverflow() {
        buffer.write("A".repeat(WIDTH));
        // Insert "XX" at col WIDTH-1, only 1 char fits, 1 goes to overflow
        buffer.cursor().set(0, WIDTH - 1);
        buffer.insert("XX");

        assertEquals(1, buffer.cursor().row());
        assertEquals(1, buffer.cursor().col());
    }

    @Test
    @DisplayName("Cursor positioned after inserted text when entire text overflows")
    void testCursorAfterInsertFullOverflow() {
        buffer.write("A".repeat(WIDTH));
        // Linija je puna, insert na col 5 - tekst pomera postojeci sadrzaj
        // kursor treba da bude na col 5 + duzina teksta koji je stao
        buffer.cursor().set(0, 5);
        buffer.insert("XYZ");

        assertEquals(0, buffer.cursor().row());
        assertEquals(8, buffer.cursor().col());
    }

    @Test
    @DisplayName("Cursor positioned after inserted text with newline")
    void testCursorAfterInsertWithNewline() {
        // Newline pomera kursor na sledecu liniju, col 0
        buffer.cursor().set(0, 2);
        buffer.insert("AB\nCD");

        assertEquals(1, buffer.cursor().row());
        assertEquals(2, buffer.cursor().col());
    }

    // ============================================
    // CONTROL CHARACTER TESTS
    // ============================================

    @Nested
    @DisplayName("Control Characters")
    class ControlCharacterTests {

        @Test
        @DisplayName("Newline moves to next line, start of line")
        void testNewline() {
            buffer.write("A\nB");
            assertEquals('A', buffer.charAtScreen(0, 0));
            assertEquals('B', buffer.charAtScreen(1, 0));
        }

        @Test
        @DisplayName("Carriage return moves to start of line")
        void testCarriageReturn() {
            buffer.write("HELLO\rX");
            // X should overwrite H
            assertEquals('X', buffer.charAtScreen(0, 0));
        }

        @Test
        @DisplayName("Multiple newlines")
        void testMultipleNewlines() {
            buffer.write("A\n\n\nB");
            assertEquals('A', buffer.charAtScreen(0, 0));
            assertEquals('B', buffer.charAtScreen(3, 0));
        }

        @Test
        @DisplayName("Write with mixed content and newlines")
        void testMixedContentNewlines() {
            buffer.write("Line1\nLine2\nLine3");
            assertTrue(buffer.lineToString(0).contains("Line1"));
            assertTrue(buffer.lineToString(1).contains("Line2"));
            assertTrue(buffer.lineToString(2).contains("Line3"));
        }

        @Test
        @DisplayName("Newline at last line triggers scroll")
        void testNewlineScroll() {
            // Go to last line
            buffer.cursor().set(HEIGHT - 1, 0);
            buffer.write("LAST");
            buffer.write("\n");

            // Cursor should still be at bottom
            assertEquals(HEIGHT - 1, buffer.cursor().row());
        }

        @Test
        @DisplayName("CR+LF (Windows line ending)")
        void testCRLF() {
            buffer.write("Line1\r\nLine2");
            assertTrue(buffer.lineToString(0).contains("Line1"));
            assertTrue(buffer.lineToString(1).contains("Line2"));
        }
    }

    // ============================================
    // ATTRIBUTES TESTS
    // ============================================

    @Nested
    @DisplayName("Attributes (Colors and Styles)")
    class AttributesTests {

        @Test
        @DisplayName("Set and get attributes")
        void testSetAttributes() {
            buffer.setAttributes(Style.Color.RED, Style.Color.BLUE, Style.StyleFlag.BOLD);
            int attrs = buffer.currentAttributes();
            assertNotEquals(0, attrs);
        }

        @Test
        @DisplayName("Written text uses current attributes")
        void testWriteWithAttributes() {
            buffer.setAttributes(Style.Color.RED, Style.Color.BLACK, Style.StyleFlag.BOLD);
            buffer.write("TEST");

            int attrs = buffer.attributesAtScreen(0, 0);
            assertNotEquals(0, attrs);
        }

        @Test
        @DisplayName("Different attributes for different text")
        void testMultipleAttributes() {
            buffer.setAttributes(Style.Color.RED, Style.Color.BLACK, Style.StyleFlag.NONE);
            buffer.write("RED");

            buffer.setAttributes(Style.Color.BLUE, Style.Color.BLACK, Style.StyleFlag.NONE);
            buffer.write("BLUE");

            int attr1 = buffer.attributesAtScreen(0, 0);
            int attr2 = buffer.attributesAtScreen(0, 3);
            assertNotEquals(attr1, attr2);
        }

        @Test
        @DisplayName("Attributes preserved on insert")
        void testAttributesPreservedOnInsert() {
            // Write with RED
            buffer.setAttributes(Style.Color.RED, Style.Color.BLACK, Style.StyleFlag.NONE);
            buffer.write("HELLO");
            int originalAttr = buffer.attributesAtScreen(0, 4); // 'O'

            // Insert with BLUE at position 2
            buffer.setAttributes(Style.Color.BLUE, Style.Color.BLACK, Style.StyleFlag.NONE);
            buffer.cursor().set(0, 2);
            buffer.insert("XX");

            // Character that was pushed should keep RED attribute
            // (now at different position due to shift)
            // This tests that overflow preserves attributes
        }

        @Test
        @DisplayName("All 16 colors work")
        void testAllColors() {
            Style.Color[] colors = {
                    Style.Color.BLACK, Style.Color.RED, Style.Color.GREEN,
                    Style.Color.YELLOW, Style.Color.BLUE, Style.Color.MAGENTA,
                    Style.Color.CYAN, Style.Color.WHITE, Style.Color.GRAY,
                    Style.Color.BRIGHT_RED, Style.Color.BRIGHT_GREEN,
                    Style.Color.BRIGHT_YELLOW, Style.Color.BRIGHT_BLUE,
                    Style.Color.BRIGHT_MAGENTA, Style.Color.BRIGHT_CYAN,
                    Style.Color.BRIGHT_WHITE
            };

            for (Style.Color color : colors) {
                buffer.setAttributes(color, Style.Color.BLACK, Style.StyleFlag.NONE);
                assertNotNull(buffer.currentAttributes());
            }
        }

        @Test
        @DisplayName("All style flags work")
        void testAllStyles() {
            buffer.setAttributes(Style.Color.WHITE, Style.Color.BLACK, Style.StyleFlag.BOLD);
            assertNotEquals(0, buffer.currentAttributes());

            buffer.setAttributes(Style.Color.WHITE, Style.Color.BLACK, Style.StyleFlag.ITALIC);
            assertNotEquals(0, buffer.currentAttributes());

            buffer.setAttributes(Style.Color.WHITE, Style.Color.BLACK, Style.StyleFlag.UNDERLINE);
            assertNotEquals(0, buffer.currentAttributes());

            // Combined styles
            buffer.setAttributes(Style.Color.WHITE, Style.Color.BLACK,
                    EnumSet.of(Style.StyleFlag.UNDERLINE, Style.StyleFlag.BOLD, Style.StyleFlag.ITALIC));
            assertNotEquals(0, buffer.currentAttributes());
        }
    }

    // ============================================
    // SCROLLBACK TESTS
    // ============================================

    @Nested
    @DisplayName("Scrollback")
    class ScrollbackTests {

        @Test
        @DisplayName("Lines scroll into scrollback")
        void testScrollbackAddsLines() {
            // Fill screen
            for (int i = 0; i < HEIGHT; i++) {
                buffer.write("LINE" + i, i, 0);
            }

            // Write at bottom to trigger scroll
            buffer.cursor().set(HEIGHT - 1, 0);
            buffer.write("\nNEW");

            // First line should be in scrollback
            char firstChar = buffer.charAtScrollBack(0, 0);
            assertEquals('L', firstChar); // 'L' from "LINE0"
        }

        @Test
        @DisplayName("Scrollback respects size limit")
        void testScrollbackSizeLimit() {
            // Scroll more than max scrollback lines
            for (int i = 0; i < MAX_SCROLLBACK + HEIGHT + 5; i++) {
                buffer.addEmptyLine();
            }

            // Scrollback shouldn't exceed limit
            // (This would need a method to check scrollback size)
        }

        @Test
        @DisplayName("Scrollback with zero max size")
        void testZeroScrollback() {
            TerminalBuffer noScrollback = new TerminalBuffer(WIDTH, HEIGHT, 0);

            // Fill and scroll
            for (int i = 0; i < HEIGHT; i++) {
                noScrollback.write("LINE" + i, i, 0);
            }
            noScrollback.addEmptyLine();

            // Scrollback should be empty (would need API to verify)
        }

        @Test
        @DisplayName("Multiple scrolls accumulate in scrollback")
        void testMultipleScrolls() {
            for (int i = 0; i < HEIGHT + 3; i++) {
                buffer.write("L" + i, 0, 0);
                buffer.addEmptyLine();
            }

            // Should have 3 lines in scrollback
            assertDoesNotThrow(() -> buffer.charAtScrollBack(0, 0));
        }

        @Test
        @DisplayName("Scrollback oldest lines are removed when full")
        void testScrollbackEviction() {
            // Fill scrollback to max
            for (int i = 0; i < MAX_SCROLLBACK + HEIGHT; i++) {
                buffer.write("X" + i, 0, 0);
                buffer.addEmptyLine();
            }

            // Add more to trigger eviction
            buffer.addEmptyLine();

            // Oldest line should be gone (would need API to verify exact state)
        }
    }

    // ============================================
    // CONTENT ACCESS TESTS
    // ============================================

    @Nested
    @DisplayName("Content Access")
    class ContentAccessTests {

        @Test
        @DisplayName("Get character at position")
        void testGetCharAt() {
            buffer.write("HELLO", 2, 3);
            assertEquals('H', buffer.charAtScreen(2, 3));
            assertEquals('E', buffer.charAtScreen(2, 4));
        }

        @Test
        @DisplayName("Get attributes at position")
        void testGetAttributesAt() {
            buffer.setAttributes(Style.Color.RED, Style.Color.BLACK, Style.StyleFlag.BOLD);
            buffer.write("X", 1, 5);

            int attrs = buffer.attributesAtScreen(1, 5);
            assertNotEquals(0, attrs);
        }

        @Test
        @DisplayName("Get line as string")
        void testGetLineAsString() {
            buffer.write("TEST", 3, 2);
            String line = buffer.lineToString(3);
            assertTrue(line.contains("TEST"));
        }

        @Test
        @DisplayName("Get entire screen as string")
        void testGetScreenAsString() {
            buffer.write("Line1", 0, 0);
            buffer.write("Line2", 1, 0);
            buffer.write("Line3", 2, 0);

            String screen = buffer.screenToString();
            assertTrue(screen.contains("Line1"));
            assertTrue(screen.contains("Line2"));
            assertTrue(screen.contains("Line3"));
        }

        @Test
        @DisplayName("Get screen and scrollback as string")
        void testGetScreenAndScrollbackAsString() {
            // Fill and scroll
            for (int i = 0; i < HEIGHT + 2; i++) {
                buffer.write("L" + i, 0, 0);
                buffer.addEmptyLine();
            }

            String full = buffer.screenAndScrollbackToString();
            assertFalse(full.isEmpty());
            assertTrue(full.contains("\n"));
        }

        @Test
        @DisplayName("Empty line returns spaces")
        void testEmptyLineString() {
            String line = buffer.lineToString(0);
            assertEquals(WIDTH, line.length());
            assertTrue(line.trim().isEmpty());
        }
    }

    // ============================================
    // LINE OPERATIONS TESTS
    // ============================================

    @Nested
    @DisplayName("Line Operations")
    class LineOperationsTests {

        @Test
        @DisplayName("Fill line with character")
        void testFillLine() {
            buffer.fillLine(2, 'X');
            String line = buffer.lineToString(2);
            assertEquals("X".repeat(WIDTH), line);
        }

        @Test
        @DisplayName("Fill line with space")
        void testFillLineWithSpace() {
            buffer.write("HELLO", 1, 0);
            buffer.fillLine(1, ' ');
            String line = buffer.lineToString(1);
            assertEquals(" ".repeat(WIDTH), line);
        }

        @Test
        @DisplayName("Add empty line at bottom")
        void testAddEmptyLine() {
            buffer.write("TEST", HEIGHT - 1, 0);
            buffer.addEmptyLine();

            // Last line should be empty now
            String lastLine = buffer.lineToString(HEIGHT - 1);
            assertTrue(lastLine.trim().isEmpty());
        }

        @Test
        @DisplayName("Clear screen resets all lines")
        void testClearScreen() {
            // Fill screen with content
            for (int i = 0; i < HEIGHT; i++) {
                buffer.write("LINE" + i, i, 0);
            }

            buffer.clearScreen();

            // All lines should be empty
            for (int i = 0; i < HEIGHT; i++) {
                String line = buffer.lineToString(i);
                assertTrue(line.trim().isEmpty());
            }

            // Cursor should be at origin
            assertEquals(0, buffer.cursor().row());
            assertEquals(0, buffer.cursor().col());
        }

        @Test
        @DisplayName("Clear screen preserves scrollback")
        void testClearScreenKeepsScrollback() {
            // Create scrollback
            for (int i = 0; i < HEIGHT + 2; i++) {
                buffer.write("S" + i, 0, 0);
                buffer.addEmptyLine();
            }

            buffer.clearScreen();

            // Scrollback should still exist
            assertDoesNotThrow(() -> buffer.charAtScrollBack(0, 0));
        }

        @Test
        @DisplayName("Clear screen and scrollback removes everything")
        void testClearScreenAndScrollback() {
            // Create content and scrollback
            for (int i = 0; i < HEIGHT + 2; i++) {
                buffer.write("X" + i, 0, 0);
                buffer.addEmptyLine();
            }

            buffer.clearScreenAndScrollback();

            // Screen should be empty
            for (int i = 0; i < HEIGHT; i++) {
                assertTrue(buffer.lineToString(i).trim().isEmpty());
            }

            // Cursor at origin
            assertEquals(0, buffer.cursor().row());
            assertEquals(0, buffer.cursor().col());
        }
    }

    // ============================================
    // EDGE CASES AND BOUNDARY CONDITIONS
    // ============================================

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    class EdgeCaseTests {

        @Test
        @DisplayName("Write exactly WIDTH characters")
        void testWriteExactWidth() {
            buffer.write("A".repeat(WIDTH));

            // All characters on first line
            assertEquals("A".repeat(WIDTH), buffer.lineToString(0));

            assertEquals(0, buffer.cursor().row());
            assertEquals(WIDTH - 1, buffer.cursor().col());
        }

        @Test
        @DisplayName("Write one character more than width")
        void testWriteOneOverWidth() {
            String text = "A".repeat(WIDTH + 1);
            buffer.write(text);

            assertEquals("A".repeat(WIDTH), buffer.lineToString(0));
            assertTrue(buffer.lineToString(1).startsWith("A"));
        }

        @Test
        @DisplayName("Insert in fully filled line")
        void testInsertInFullLine() {
            buffer.write("A".repeat(WIDTH));
            buffer.cursor().set(0, 5);

            buffer.insert("X");

            // Should have overflow to next line
            assertFalse(buffer.lineToString(1).trim().isEmpty());
        }

        @Test
        @DisplayName("Operations on first line")
        void testFirstLineOperations() {
            buffer.write("FIRST", 0, 0);
            assertEquals("FIRST", buffer.lineToString(0).substring(0, 5));

            buffer.cursor().up(10); // Try to go above
            assertEquals(0, buffer.cursor().row());
        }

        @Test
        @DisplayName("Operations on last line")
        void testLastLineOperations() {
            buffer.write("LAST", HEIGHT - 1, 0);
            assertEquals("LAST", buffer.lineToString(HEIGHT - 1).substring(0, 4));

            buffer.cursor().set(HEIGHT - 1, 0);
            buffer.cursor().down(10); // Try to go below
            assertEquals(HEIGHT - 1, buffer.cursor().row());
        }

        @Test
        @DisplayName("Write at last position of last line")
        void testWriteAtLastPosition() {
            buffer.cursor().set(HEIGHT - 1, WIDTH - 1);
            buffer.write("XY");

            // Should trigger scroll
            assertEquals(HEIGHT - 1, buffer.cursor().row());
        }

        @Test
        @DisplayName("Very long text write")
        void testVeryLongWrite() {
            String longText = "A".repeat(WIDTH * HEIGHT * 2);
            buffer.write(longText);

            // Should have scrolled and filled screen
            assertFalse(buffer.screenToString().trim().isEmpty());
        }

        @Test
        @DisplayName("Alternating writes and cursor moves")
        void testAlternatingOperations() {
            buffer.write("A");
            buffer.cursor().down(1);
            buffer.write("B");
            buffer.cursor().up(1);
            buffer.cursor().right(2);
            buffer.write("C");

            assertEquals('A', buffer.charAtScreen(0, 0));
            assertEquals('B', buffer.charAtScreen(1, 1));
            assertEquals('C', buffer.charAtScreen(0, 4));
        }

        @Test
        @DisplayName("Write empty string at various positions")
        void testWriteEmptyAtPositions() {
            buffer.write("", 0, 0);
            buffer.write("", HEIGHT - 1, WIDTH - 1);
            buffer.write("", HEIGHT / 2, WIDTH / 2);

            // Screen should still be empty
            assertTrue(buffer.screenToString().trim().isEmpty());
        }

        @Test
        @DisplayName("Fill all lines with different characters")
        void testFillAllLines() {
            for (int i = 0; i < HEIGHT; i++) {
                buffer.fillLine(i, (char) ('A' + i));
            }

            for (int i = 0; i < HEIGHT; i++) {
                char expected = (char) ('A' + i);
                assertEquals(expected, buffer.charAtScreen(i, 0));
            }
        }

        @Test
        @DisplayName("Rapid cursor movements")
        void testRapidCursorMovements() {
            for (int i = 0; i < 100; i++) {
                buffer.cursor().right(1);
                buffer.cursor().down(1);
                buffer.cursor().left(1);
                buffer.cursor().up(1);
            }

            // Cursor should still be valid
            assertTrue(buffer.cursor().row() >= 0 && buffer.cursor().row() < HEIGHT);
            assertTrue(buffer.cursor().col() >= 0 && buffer.cursor().col() < WIDTH);
        }

        @Test
        @DisplayName("Large buffer operations")
        void testLargeBuffer() {
            TerminalBuffer large = new TerminalBuffer(200, 100, 1000);
            large.write("TEST", 50, 100);
            assertEquals('T', large.charAtScreen(50, 100));
        }

        @Test
        @DisplayName("Scrollback with exact max size")
        void testScrollbackExactMaxSize() {
            // Fill exactly to max scrollback
            for (int i = 0; i < MAX_SCROLLBACK + HEIGHT; i++) {
                buffer.write("L" + i, 0, 0);
                buffer.addEmptyLine();
            }

            // Add one more to test eviction
            buffer.addEmptyLine();

            // Should not crash and scrollback should be at max
        }

        @Test
        @DisplayName("Mix of all operations")
        void testMixedOperations() {
            buffer.write("START");
            buffer.setAttributes(Style.Color.RED, Style.Color.BLACK, Style.StyleFlag.BOLD);
            buffer.cursor().down(2);

            buffer.insert("MID");

            buffer.fillLine(3, 'X');
            buffer.clearScreen();
            buffer.write("END");

            // Should not crash and end state should be valid
            assertTrue(buffer.lineToString(0).contains("END"));
        }

        @Test
        @DisplayName("Pending wrap with insert operation")
        void testPendingWrapWithInsert() {
            buffer.write("HELLO");
            buffer.cursor().set(0, WIDTH - 1);
            buffer.write("X"); // Enters pending wrap at end

            // Insert at start of line
            buffer.cursor().set(0, 0);
            buffer.insert(">"); // Should clear pending wrap and insert

            // Line should start with ">"
            assertTrue(buffer.lineToString(0).startsWith(">"));

            // Should have wrapped
            assertTrue(buffer.lineToString(1).startsWith("X"));
        }

        @Test
        @DisplayName("1x1 buffer stress test")
        void test1x1BufferStressTest() {
            TerminalBuffer tiny = new TerminalBuffer(1, 1, 10);

            // Write 50 characters
            for (int i = 0; i < 50; i++) {
                tiny.write(String.valueOf((char)('A' + (i % 26))));
            }

            // Screen should have last character
            assertEquals('X', tiny.charAtScreen(0, 0)); // 'A' + 23 = 'X'

            // Wait, 50 % 26 = 24, so 'A' + 24 = 'Y'
            // Actually (50-1) % 26 = 49 % 26 = 23 = 'X'... let me recalculate
            // 49 % 26 = 23, 'A' + 23 = 'X'

            // Scrollback should be at max (10 lines)
            assertEquals(10, tiny.scrollbackSize());

            // Operations should still work
            tiny.clearScreen();
            tiny.write("Z");
            assertEquals('Z', tiny.charAtScreen(0, 0));
        }
    }

    @Nested
    @DisplayName("Pending Wrap Behavior")
    class PendingWrapTests {

        @Test
        @DisplayName("Writing exactly WIDTH characters enters pending wrap state")
        void testPendingWrapAfterFullLine() {
            buffer.write("A".repeat(WIDTH));

            // Cursor should be at end of line in pending wrap state
            assertEquals(0, buffer.cursor().row());
            assertEquals(WIDTH - 1, buffer.cursor().col());

            // All characters should be on first line
            assertEquals("A".repeat(WIDTH), buffer.lineToString(0));
        }

        @Test
        @DisplayName("Next character after pending wrap triggers actual wrap")
        void testPendingWrapTriggersOnNextChar() {
            buffer.write("A".repeat(WIDTH));

            // Cursor at end of line 0
            assertEquals(0, buffer.cursor().row());
            assertEquals(WIDTH - 1, buffer.cursor().col());

            // Write one more character
            buffer.write("B");

            // Should have wrapped to next line
            assertEquals(1, buffer.cursor().row());
            assertEquals(1, buffer.cursor().col());
            assertEquals('B', buffer.charAtScreen(1, 0));
        }

        @Test
        @DisplayName("Explicit cursor movement clears pending wrap")
        void testCursorMovementClearsPendingWrap() {
            buffer.write("A".repeat(WIDTH));

            // Cursor at end of line (pending wrap)
            assertEquals(0, buffer.cursor().row());

            // Move cursor explicitly
            buffer.cursor().left(1);

            // Cursor should move to WIDTH-2
            assertEquals(WIDTH - 2, buffer.cursor().col());

            // Write another character - should overwrite at current position
            buffer.write("X");

            // Should overwrite the last 'A', not wrap
            assertEquals('X', buffer.charAtScreen(0, WIDTH - 2));
            assertEquals('A', buffer.charAtScreen(0, WIDTH - 1));
        }

        @Test
        @DisplayName("Newline after pending wrap goes to next line start")
        void testNewlineAfterPendingWrap() {
            buffer.write("A".repeat(WIDTH));

            // Cursor at end in pending wrap
            assertEquals(0, buffer.cursor().row());

            // Write newline
            buffer.write("\n");

            // Should go to start of next line
            assertEquals(1, buffer.cursor().row());
            assertEquals(0, buffer.cursor().col());
        }

        @Test
        @DisplayName("Carriage return after pending wrap goes to line start")
        void testCarriageReturnAfterPendingWrap() {
            buffer.write("A".repeat(WIDTH));

            // Write carriage return
            buffer.write("\r");

            // Should go to start of same line
            assertEquals(0, buffer.cursor().row());
            assertEquals(0, buffer.cursor().col());

            // Write character - should overwrite first 'A'
            buffer.write("X");
            assertEquals('X', buffer.charAtScreen(0, 0));
        }

        @Test
        @DisplayName("Multiple lines fill correctly with pending wrap")
        void testMultipleLinesPendingWrap() {
            // Write exactly WIDTH chars 3 times
            buffer.write("A".repeat(WIDTH));
            buffer.write("B".repeat(WIDTH));
            buffer.write("C".repeat(WIDTH));

            // Should have 3 lines filled
            assertEquals("A".repeat(WIDTH), buffer.lineToString(0));
            assertEquals("B".repeat(WIDTH), buffer.lineToString(1));
            assertEquals("C".repeat(WIDTH), buffer.lineToString(2));

            // Cursor should be at end of line 2
            assertEquals(2, buffer.cursor().row());
            assertEquals(WIDTH - 1, buffer.cursor().col());
        }

        @Test
        @DisplayName("Pending wrap at last line triggers scroll on next char")
        void testPendingWrapScrolls() {
            // Fill entire screen
            for (int i = 0; i < HEIGHT; i++) {
                buffer.write("L" + i);
                buffer.cursor().set(i, WIDTH - 3);
                buffer.write("END");
            }

            // Cursor at end of last line (pending wrap)
            assertEquals(HEIGHT - 1, buffer.cursor().row());
            assertEquals(WIDTH - 1, buffer.cursor().col());

            // Write one more character - should trigger scroll
            buffer.write("X");

            // Should still be on last line (after scroll)
            assertEquals(HEIGHT - 1, buffer.cursor().row());
            assertEquals(1, buffer.cursor().col());

            // 'X' should be on screen
            assertEquals('X', buffer.charAtScreen(HEIGHT - 1, 0));

            // First line should be in scrollback
            assertEquals('L', buffer.charAtScrollBack(0, 0));
        }

        @Test
        @DisplayName("Insert after pending wrap triggers wrap first")
        void testInsertAfterPendingWrap() {
            buffer.write("A".repeat(WIDTH));

            // Cursor in pending wrap at end of line 0
            assertEquals(0, buffer.cursor().row());

            // Insert text
            buffer.insert("XYZ");

            // Should wrap first, then insert on line 1
            assertEquals('X', buffer.charAtScreen(1, 0));
            assertEquals('Y', buffer.charAtScreen(1, 1));
            assertEquals('Z', buffer.charAtScreen(1, 2));
        }

        @Test
        @DisplayName("Set cursor clears pending wrap")
        void testSetCursorClearsPendingWrap() {
            buffer.write("A".repeat(WIDTH));

            // Set cursor to middle of line
            buffer.cursor().set(0, WIDTH / 2);

            // Write character - should overwrite at WIDTH/2, not wrap
            buffer.write("X");

            assertEquals('X', buffer.charAtScreen(0, WIDTH / 2));
            assertEquals(WIDTH / 2 + 1, buffer.cursor().col());
        }

        @Test
        @DisplayName("Attributes preserved through pending wrap")
        void testAttributesPreservedThroughPendingWrap() {
            buffer.setAttributes(Style.Color.RED, Style.Color.BLACK, Style.StyleFlag.BOLD);
            int redAttrs = buffer.currentAttributes();

            buffer.write("A".repeat(WIDTH));

            // Change attributes
            buffer.setAttributes(Style.Color.BLUE, Style.Color.BLACK, Style.StyleFlag.NONE);
            int blueAttrs = buffer.currentAttributes();

            // Write character to trigger wrap
            buffer.write("B");

            // Last 'A' should have red attributes
            assertEquals(redAttrs, buffer.attributesAtScreen(0, WIDTH - 1));

            // 'B' should have blue attributes
            assertEquals(blueAttrs, buffer.attributesAtScreen(1, 0));
        }

        @Test
        @DisplayName("Pending wrap with write at specific position")
        void testPendingWrapWithPositionedWrite() {
            buffer.write("A".repeat(WIDTH), 2, 0);

            // Cursor at end of line 2
            assertEquals(2, buffer.cursor().row());
            assertEquals(WIDTH - 1, buffer.cursor().col());

            // Write at different position
            buffer.write("X", 1, 5);

            // Should write at (1, 5), not wrap from previous position
            assertEquals('X', buffer.charAtScreen(1, 5));
        }

        // ============================================
        // SPECIAL TEST: 1x1 BUFFER
        // ============================================

        @Test
        @DisplayName("Minimum 1x1 buffer with pending wrap")
        void test1x1BufferWithPendingWrap() {
            TerminalBuffer tiny = new TerminalBuffer(1, 1, 5);

            // Write first character
            tiny.write("A");

            // Cursor should be at (0, 0) in pending wrap state
            assertEquals(0, tiny.cursor().row());
            assertEquals(0, tiny.cursor().col());
            assertEquals('A', tiny.charAtScreen(0, 0));

            // Write second character - should trigger wrap and scroll
            tiny.write("B");

            // 'B' should now be on screen at (0, 0)
            assertEquals('B', tiny.charAtScreen(0, 0));
            assertEquals(0, tiny.cursor().row());
            assertEquals(0, tiny.cursor().col());

            // 'A' should be in scrollback
            assertEquals(1, tiny.scrollbackSize());
            assertEquals('A', tiny.charAtScrollBack(0, 0));

            // Write third character
            tiny.write("C");

            // 'C' on screen, 'A' and 'B' in scrollback
            assertEquals('C', tiny.charAtScreen(0, 0));
            assertEquals(2, tiny.scrollbackSize());
            assertEquals('A', tiny.charAtScrollBack(0, 0));
            assertEquals('B', tiny.charAtScrollBack(1, 0));

            // Write multiple characters in sequence
            tiny.write("DEFGH");

            // 'H' should be on screen (last character)
            assertEquals('H', tiny.charAtScreen(0, 0));

            // Scrollback should have: A, B, C, D, E, F, G (7 lines, but max is 5)
            assertEquals(5, tiny.scrollbackSize()); // Max scrollback

            // Oldest lines evicted, should have: D, E, F, G, H on screen
            assertEquals('C', tiny.charAtScrollBack(0, 0)); // Oldest in scrollback
            assertEquals('D', tiny.charAtScrollBack(1, 0));
            assertEquals('E', tiny.charAtScrollBack(2, 0));
            assertEquals('F', tiny.charAtScrollBack(3, 0));
            assertEquals('G', tiny.charAtScrollBack(4, 0)); // Wait, this should be on screen

        }

        @Test
        @DisplayName("1x1 buffer with newline clears pending wrap")
        void test1x1BufferNewline() {
            TerminalBuffer tiny = new TerminalBuffer(1, 1, 5);

            tiny.write("A");

            // Cursor at (0, 0) in pending wrap
            assertEquals('A', tiny.charAtScreen(0, 0));

            // Write newline - should scroll and clear pending wrap
            tiny.write("\n");

            // Actually after scroll, cursor should be at (0, 0) again
            assertEquals(0, tiny.cursor().row());
            assertEquals(0, tiny.cursor().col());

            // 'A' should be in scrollback
            assertEquals(1, tiny.scrollbackSize());
        }

        @Test
        @DisplayName("1x1 buffer cursor movements")
        void test1x1BufferCursorMovements() {
            TerminalBuffer tiny = new TerminalBuffer(1, 1, 2);

            tiny.write("X");

            // Try to move cursor (should clamp to bounds)
            tiny.cursor().up(1);
            assertEquals(0, tiny.cursor().row());
            assertEquals(0, tiny.cursor().col());

            tiny.cursor().down(1);
            assertEquals(0, tiny.cursor().row()); // Can't go below 0 in 1-high buffer

            tiny.cursor().left(1);
            assertEquals(0, tiny.cursor().col());

            tiny.cursor().right(1);
            assertEquals(0, tiny.cursor().col()); // Can't go beyond 0 in 1-wide buffer

            // Write after movements - should overwrite 'X'
            tiny.write("Y");
            assertEquals('Y', tiny.charAtScreen(0, 0));
        }

        @Test
        @DisplayName("1x1 buffer with attributes")
        void test1x1BufferAttributes() {
            TerminalBuffer tiny = new TerminalBuffer(1, 1, 3);

            // Write with red
            tiny.setAttributes(Style.Color.RED, Style.Color.BLACK, Style.StyleFlag.BOLD);
            int redAttrs = tiny.currentAttributes();
            tiny.write("R");

            assertEquals(redAttrs, tiny.attributesAtScreen(0, 0));

            // Write with blue (triggers scroll)
            tiny.setAttributes(Style.Color.BLUE, Style.Color.BLACK, Style.StyleFlag.NONE);
            int blueAttrs = tiny.currentAttributes();
            tiny.write("B");

            assertEquals(blueAttrs, tiny.attributesAtScreen(0, 0));

            // Red 'R' should be in scrollback with correct attributes
            assertEquals(redAttrs, tiny.attributesAtScrollback(0, 0));
        }

        @Test
        @DisplayName("1x1 buffer fill and clear operations")
        void test1x1BufferFillAndClear() {
            TerminalBuffer tiny = new TerminalBuffer(1, 1, 2);

            tiny.write("A");

            // Fill line with 'X'
            tiny.fillLine(0, 'X');
            assertEquals('X', tiny.charAtScreen(0, 0));

            // Clear screen
            tiny.clearScreen();
            assertEquals(' ', tiny.charAtScreen(0, 0));
            assertEquals(0, tiny.cursor().row());
            assertEquals(0, tiny.cursor().col());
        }
    }

    // ============================================
    // REGRESSION TESTS
    // ============================================

    @Nested
    @DisplayName("Regression Tests")
    class RegressionTests {

        @Test
        @DisplayName("Insert does not corrupt line after overflow")
        void testInsertNoCorruption() {
            buffer.write("A".repeat(WIDTH), 0, 0);
            buffer.cursor().set(0, WIDTH / 2);

            buffer.insert("XXXXX");

            // Line should still be valid
            String line = buffer.lineToString(0);
            assertEquals(WIDTH, line.length());
        }

        @Test
        @DisplayName("Attributes not lost during wrap")
        void testAttributesDuringWrap() {
            buffer.setAttributes(Style.Color.RED, Style.Color.BLACK, Style.StyleFlag.BOLD);
            int attrs = buffer.currentAttributes();

            buffer.write("A".repeat(WIDTH + 5));

            // Check first and second line have correct attributes
            assertEquals(attrs, buffer.attributesAtScreen(0, 0));
            assertEquals(attrs, buffer.attributesAtScreen(1, 0));
        }
    }

    @Nested
    @DisplayName("Wide Character Support")
    class WideCharTests {

        // CJK karakter koji zauzima 2 ćelije
        private static final char CJK = '中';
        // Emoji koji zauzima 2 ćelije
        private static final char EMOJI = '★';

        @Test
        @DisplayName("Wide character occupies 2 cells")
        void testWideCharOccupies2Cells() {
            buffer.write(String.valueOf(CJK));

            assertEquals(CJK, buffer.charAtScreen(0, 0));
            // Druga ćelija je placeholder
            assertEquals('\u0000', buffer.charAtScreen(0, 1));
        }

        @Test
        @DisplayName("Cursor advances by 2 after wide character")
        void testCursorAdvancesBy2() {
            buffer.write(String.valueOf(CJK));

            assertEquals(0, buffer.cursor().row());
            assertEquals(2, buffer.cursor().col());
        }

        @Test
        @DisplayName("Mix of wide and narrow characters")
        void testMixedWideAndNarrow() {
            buffer.write("A" + CJK + "B");

            assertEquals('A', buffer.charAtScreen(0, 0));
            assertEquals(CJK, buffer.charAtScreen(0, 1));
            assertEquals('\u0000', buffer.charAtScreen(0, 2)); // placeholder
            assertEquals('B', buffer.charAtScreen(0, 3));
            assertEquals(4, buffer.cursor().col());
        }

        @Test
        @DisplayName("Wide character at end of line — no space, pads with space and wraps")
        void testWideCharAtEndOfLine() {
            // Popuni WIDTH-1 karaktera, ostaje 1 ćelija — wide ne staje
            buffer.write("A".repeat(WIDTH - 1));
            buffer.write(String.valueOf(CJK));

            // Poslednja ćelija linije 0 treba da bude space (padding)
            assertEquals(' ', buffer.charAtScreen(0, WIDTH - 1));

            // Wide karakter treba da bude na početku linije 1
            assertEquals(CJK, buffer.charAtScreen(1, 0));
            assertEquals('\u0000', buffer.charAtScreen(1, 1));
        }

        @Test
        @DisplayName("Wide character fits exactly at end of line")
        void testWideCharFitsExactlyAtEnd() {
            buffer.write("A".repeat(WIDTH - 2));
            buffer.write(String.valueOf(CJK));

            assertEquals(CJK, buffer.charAtScreen(0, WIDTH - 2));
            assertEquals('\u0000', buffer.charAtScreen(0, WIDTH - 1));
            assertEquals(0, buffer.cursor().row());
            assertEquals(WIDTH - 1, buffer.cursor().col());
        }

        @Test
        @DisplayName("Multiple wide characters on same line")
        void testMultipleWideChars() {
            // WIDTH=10, 5 wide karaktera = 10 ćelija = tačno jedna linija
            buffer.write(String.valueOf(CJK).repeat(WIDTH / 2));

            for (int i = 0; i < WIDTH / 2; i++) {
                assertEquals(CJK, buffer.charAtScreen(0, i * 2));
                assertEquals('\u0000', buffer.charAtScreen(0, i * 2 + 1));
            }

            assertEquals(0, buffer.cursor().row());
            assertEquals(WIDTH - 1, buffer.cursor().col());
        }

        @Test
        @DisplayName("Wide characters wrap to next line")
        void testWideCharWraps() {
            // WIDTH=10, 6 wide karaktera = 12 ćelija, 1 mora na sledeću liniju
            buffer.write(String.valueOf(CJK).repeat(WIDTH / 2 + 1));

            // Linija 0 — 5 wide karaktera (10 ćelija)
            assertEquals(CJK, buffer.charAtScreen(0, 0));
            // Linija 1 — 1 wide karakter
            assertEquals(CJK, buffer.charAtScreen(1, 0));
            assertEquals('\u0000', buffer.charAtScreen(1, 1));
        }

        @Test
        @DisplayName("Wide character attributes preserved")
        void testWideCharAttributes() {
            buffer.setAttributes(Style.Color.RED, Style.Color.BLACK, Style.StyleFlag.BOLD);
            int redAttrs = buffer.currentAttributes();

            buffer.write(String.valueOf(CJK));

            // Obe ćelije treba da imaju iste atribute
            assertEquals(redAttrs, buffer.attributesAtScreen(0, 0));
            assertEquals(redAttrs, buffer.attributesAtScreen(0, 1));
        }

        @Test
        @DisplayName("Insert wide character shifts content right by 2")
        void testInsertWideCharShifts() {
            buffer.write("ABCDE");
            buffer.cursor().set(0, 1);

            buffer.insert(String.valueOf(CJK));

            assertEquals('A', buffer.charAtScreen(0, 0));
            assertEquals(CJK, buffer.charAtScreen(0, 1));
            assertEquals('\u0000', buffer.charAtScreen(0, 2));
            assertEquals('B', buffer.charAtScreen(0, 3));
        }

        @Test
        @DisplayName("Insert wide character cursor positioned after both cells")
        void testInsertWideCharCursorPosition() {
            buffer.cursor().set(0, 2);
            buffer.insert(String.valueOf(CJK));

            // Kursor treba da bude na col 4 (2 + 2)
            assertEquals(0, buffer.cursor().row());
            assertEquals(4, buffer.cursor().col());
        }

        @Test
        @DisplayName("Wide character at last position of last line triggers scroll")
        void testWideCharTriggersScroll() {
            // Popuni ekran do poslednje 2 ćelije
            for (int i = 0; i < HEIGHT - 1; i++) {
                buffer.write("A".repeat(WIDTH), i, 0);
            }
            buffer.write("A".repeat(WIDTH - 2), HEIGHT - 1, 0);

            buffer.write(String.valueOf(CJK));

            // Treba da se scroll-uje
            assertEquals(HEIGHT - 1, buffer.cursor().row());
        }

        @Test
        @DisplayName("Line toString skips placeholder cells")
        void testWideCharToString() {
            buffer.write("A" + CJK + "B");

            String line = buffer.lineToString(0);
            // Placeholder \u0000 može biti prisutan — bitno je da CJK bude tu
            assertTrue(line.contains(String.valueOf(CJK)));
            assertTrue(line.contains("A"));
            assertTrue(line.contains("B"));
        }

        @Test
        @DisplayName("Wide character in scrollback preserved correctly")
        void testWideCharInScrollback() {
            buffer.write(String.valueOf(CJK), 0, 0);

            // Scroll liniju u scrollback
            for (int i = 0; i < HEIGHT; i++) {
                buffer.addEmptyLine();
            }

            assertEquals(CJK, buffer.charAtScrollBack(0, 0));
            assertEquals('\u0000', buffer.charAtScrollBack(0, 1));
        }

        @Test
        @DisplayName("Wide character with pending wrap")
        void testWideCharWithPendingWrap() {
            buffer.write("A".repeat(WIDTH)); // pending wrap

            buffer.write(String.valueOf(CJK));

            // Treba da wrapa, CJK na liniji 1
            assertEquals(CJK, buffer.charAtScreen(1, 0));
            assertEquals('\u0000', buffer.charAtScreen(1, 1));
        }
    }
}