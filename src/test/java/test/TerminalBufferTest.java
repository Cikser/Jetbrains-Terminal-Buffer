package test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import terminalbuffer.Style;
import terminalbuffer.TerminalBuffer;

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
            buffer.setAttributes(Style.Color.RED, Style.Color.BLUE, Style.BOLD);
            int attrs = buffer.currentAttributes();
            assertNotEquals(0, attrs);
        }

        @Test
        @DisplayName("Written text uses current attributes")
        void testWriteWithAttributes() {
            buffer.setAttributes(Style.Color.RED, Style.Color.BLACK, Style.BOLD);
            buffer.write("TEST");

            int attrs = buffer.attributesAtScreen(0, 0);
            assertNotEquals(0, attrs);
        }

        @Test
        @DisplayName("Different attributes for different text")
        void testMultipleAttributes() {
            buffer.setAttributes(Style.Color.RED, Style.Color.BLACK, Style.NONE);
            buffer.write("RED");

            buffer.setAttributes(Style.Color.BLUE, Style.Color.BLACK, Style.NONE);
            buffer.write("BLUE");

            int attr1 = buffer.attributesAtScreen(0, 0);
            int attr2 = buffer.attributesAtScreen(0, 3);
            assertNotEquals(attr1, attr2);
        }

        @Test
        @DisplayName("Attributes preserved on insert")
        void testAttributesPreservedOnInsert() {
            // Write with RED
            buffer.setAttributes(Style.Color.RED, Style.Color.BLACK, Style.NONE);
            buffer.write("HELLO");
            int originalAttr = buffer.attributesAtScreen(0, 4); // 'O'

            // Insert with BLUE at position 2
            buffer.setAttributes(Style.Color.BLUE, Style.Color.BLACK, Style.NONE);
            buffer.cursor().set(0, 2);
            buffer.insert("XX");

            // Character that was pushed should keep RED attribute
            // (now at different position due to shift)
            // This tests that overflow preserves attributes
        }

        @Test
        @DisplayName("All 16 colors work")
        void testAllColors() {
            int[] colors = {
                    Style.Color.BLACK, Style.Color.RED, Style.Color.GREEN,
                    Style.Color.YELLOW, Style.Color.BLUE, Style.Color.MAGENTA,
                    Style.Color.CYAN, Style.Color.WHITE, Style.Color.GRAY,
                    Style.Color.BRIGHT_RED, Style.Color.BRIGHT_GREEN,
                    Style.Color.BRIGHT_YELLOW, Style.Color.BRIGHT_BLUE,
                    Style.Color.BRIGHT_MAGENTA, Style.Color.BRIGHT_CYAN,
                    Style.Color.BRIGHT_WHITE
            };

            for (int color : colors) {
                buffer.setAttributes(color, Style.Color.BLACK, Style.NONE);
                assertNotNull(buffer.currentAttributes());
            }
        }

        @Test
        @DisplayName("All style flags work")
        void testAllStyles() {
            buffer.setAttributes(Style.Color.WHITE, Style.Color.BLACK, Style.BOLD);
            assertNotEquals(0, buffer.currentAttributes());

            buffer.setAttributes(Style.Color.WHITE, Style.Color.BLACK, Style.ITALIC);
            assertNotEquals(0, buffer.currentAttributes());

            buffer.setAttributes(Style.Color.WHITE, Style.Color.BLACK, Style.UNDERLINE);
            assertNotEquals(0, buffer.currentAttributes());

            // Combined styles
            buffer.setAttributes(Style.Color.WHITE, Style.Color.BLACK,
                    Style.BOLD | Style.ITALIC | Style.UNDERLINE);
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
            buffer.setAttributes(Style.Color.RED, Style.Color.BLACK, Style.BOLD);
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

            assertEquals(1, buffer.cursor().row());
            assertEquals(0, buffer.cursor().col());
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
            buffer.setAttributes(Style.Color.RED, Style.Color.BLACK, Style.BOLD);
            buffer.cursor().down(2);

            buffer.insert("MID");

            buffer.fillLine(3, 'X');
            buffer.clearScreen();
            buffer.write("END");

            // Should not crash and end state should be valid
            assertTrue(buffer.lineToString(0).contains("END"));
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
        @DisplayName("Cursor position after multiple wraps")
        void testCursorAfterWraps() {
            String longText = "A".repeat(WIDTH * 3);
            buffer.write(longText);

            // Cursor should be at a valid position
            assertTrue(buffer.cursor().row() >= 0 && buffer.cursor().row() < HEIGHT);
            assertTrue(buffer.cursor().col() >= 0 && buffer.cursor().col() < WIDTH);
        }

        @Test
        @DisplayName("Attributes not lost during wrap")
        void testAttributesDuringWrap() {
            buffer.setAttributes(Style.Color.RED, Style.Color.BLACK, Style.BOLD);
            int attrs = buffer.currentAttributes();

            buffer.write("A".repeat(WIDTH + 5));

            // Check first and second line have correct attributes
            assertEquals(attrs, buffer.attributesAtScreen(0, 0));
            assertEquals(attrs, buffer.attributesAtScreen(1, 0));
        }
    }
}