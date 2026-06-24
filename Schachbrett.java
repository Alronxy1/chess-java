public class Schachbrett {

   static Pad chessBoard = new Pad("Schachbrett");
   static final int SIZE  = 100;
   static final int BOARD = 8;

   static int[][] board = new int[8][8];

   static final String IMG = "U:/POSE/Schach/Chess_";

   static int selectedRow = -1;
   static int selectedCol = -1;
   static int player = 0;
   static boolean done = false;

   // Merkt ob König oder Türme bereits gezogen haben
   static boolean whiteKingMoved    = false;
   static boolean blackKingMoved    = false;
   static boolean whiteRookAMoved   = false; // Turm a1 (col 0)
   static boolean whiteRookHMoved   = false; // Turm h1 (col 7)
   static boolean blackRookAMoved   = false; // Turm a8 (col 0)
   static boolean blackRookHMoved   = false; // Turm h8 (col 7)

   public static void main(String[] args) {
      fillBoard();

      chessBoard.setPadSize(SIZE * BOARD, SIZE * BOARD);
      chessBoard.setVisible(true);

      chessBoard.addMouseListener(
         (x, y) -> {
            System.out.println("Klick auf Pixel: " + x + ", " + y);

            int col = x / SIZE;
            int row = y / SIZE;

            System.out.println("Schachfeld (Reihe/Spalte): " + row + ", " + col);

            if (!done) {
               handleMouseClick(row, col);
            }
         });

      refreshBoard();
      System.out.println("Weiß ist am Zug. Bitte auf eine Figur klicken!");
      
   }

   public static void handleMouseClick(int r, int c) {

      if (selectedRow == -1 && selectedCol == -1) {
         int piece = board[r][c];
         int minP  = player * 6;
         int maxP  = minP + 6;

         if (piece > minP && piece <= maxP) {
            selectedRow = r;
            selectedCol = c;
            refreshBoard();
         } else {
            System.out.println("Dort steht keine eigene Figur!");
         }

      } else {
         int r1 = selectedRow;
         int c1 = selectedCol;

         if (r1 == r && c1 == c) {
            selectedRow = -1;
            selectedCol = -1;
            refreshBoard();
            return;
         }

         // Rochade prüfen bevor isValidMove geprüft wird
         if (isCastlingMove(r1, c1, r, c)) {

            executeCastling(r1, c1, r, c);
            selectedRow = -1;
            selectedCol = -1;
            player = (player + 1) % 2;
            refreshBoard();
            checkGameState();

            return;
         }

         if (!isValidMove(r1, c1, r, c, board, player)) {
            System.out.println("Ungültiger Zug! Bitte erneut versuchen.");
            selectedRow = -1;
            selectedCol = -1;
            refreshBoard();
            return;
         }

         int captured = board[r][c];
         int moved = board[r1][c1];
         board[r][c] = moved;
         board[r1][c1] = 0;

         if (moved == 1 && r == 0) board[r][c] = 5;
         if (moved == 7 && r == 7) board[r][c] = 11;

         // Züge der Könige und Türme vermerken
         updateMoveFlags(r1, c1, moved);

         int kingPos = findKing(board, player);
         if (kingPos == -1 || isAttacked(board, kingPos / 8, kingPos % 8, player)) {
            System.out.println("Ungültiger Zug! Der eigene König wäre im Schach.");
            board[r1][c1] = moved;
            board[r][c]   = captured;

            // Flags zurücksetzen falls Zug rückgängig gemacht wird
            revertMoveFlags(r1, c1, moved);
            selectedRow = -1;
            selectedCol = -1;
            refreshBoard();
            return;
         }

         selectedRow = -1;
         selectedCol = -1;
         player = (player + 1) % 2;
         refreshBoard();
         checkGameState();
      }
   }

   // Flags setzen wenn König oder Turm zieht
   public static void updateMoveFlags(int r, int c, int piece) {
      if (piece == 6)  whiteKingMoved  = true;
      if (piece == 12) blackKingMoved  = true;
      if (piece == 2 && r == 7 && c == 0) whiteRookAMoved = true;
      if (piece == 2 && r == 7 && c == 7) whiteRookHMoved = true;
      if (piece == 8 && r == 0 && c == 0) blackRookAMoved = true;
      if (piece == 8 && r == 0 && c == 7) blackRookHMoved = true;
   }

   // Flags zurücksetzen wenn Zug rückgängig gemacht wird
   public static void revertMoveFlags(int r, int c, int piece) {
      if (piece == 6)  whiteKingMoved  = false;
      if (piece == 12) blackKingMoved  = false;
      if (piece == 2 && r == 7 && c == 0) whiteRookAMoved = false;
      if (piece == 2 && r == 7 && c == 7) whiteRookHMoved = false;
      if (piece == 8 && r == 0 && c == 0) blackRookAMoved = false;
      if (piece == 8 && r == 0 && c == 7) blackRookHMoved = false;
   }

   // Prüft ob der Klick eine Rochade ist (König zieht 2 Felder seitwärts)
   public static boolean isCastlingMove(int r1, int c1, int r2, int c2) {
      int piece = board[r1][c1];
      boolean isKing = (piece == 6 || piece == 12);
      if (!isKing) return false;
      if (r1 != r2) return false;
      if (Math.abs(c2 - c1) != 2) return false;
      return canCastle(r1, c1, c2);
   }

   // Prüft alle Bedingungen für die Rochade
   public static boolean canCastle(int row, int kingCol, int targetCol) {
      boolean kingside = (targetCol > kingCol);

      if (player == 0) {
         if (whiteKingMoved) return false;
         if (kingside && whiteRookHMoved) return false;
         if (!kingside && whiteRookAMoved) return false;
      } else {
         if (blackKingMoved) return false;
         if (kingside && blackRookHMoved) return false;
         if (!kingside && blackRookAMoved) return false;
      }

      // Felder zwischen König und Turm müssen frei sein
      int rookCol = kingside ? 7 : 0;
      int step = kingside ? 1 : -1;

      for (int c = kingCol + step; c != rookCol; c += step) {
         if (board[row][c] != 0) return false;
      }

      // König darf nicht im Schach stehen und kein Feld überqueren das angegriffen ist
      if (isAttacked(board, row, kingCol, player)) return false;
      if (isAttacked(board, row, kingCol + step, player)) return false;
      if (isAttacked(board, row, kingCol + 2 * step, player)) return false;

      return true;
   }

   // Führt die Rochade aus (König und Turm verschieben)
   public static void executeCastling(int r1, int c1, int r2, int c2) {

      boolean kingside = (c2 > c1);
      int rookFromCol = kingside ? 7 : 0;
      int rookToCol   = kingside ? 5 : 3;

      board[r2][c2] = board[r1][c1]; // König bewegen
      board[r1][c1] = 0;
      board[r1][rookToCol] = board[r1][rookFromCol]; // Turm bewegen
      board[r1][rookFromCol] = 0;

      if (player == 0) {
         whiteKingMoved = true;
         if (kingside) {
            whiteRookHMoved = true;
         } else {
            whiteRookAMoved = true;
         }

      } else {
         blackKingMoved = true;

         if (kingside) {
            blackRookHMoved = true;
         } else {
            blackRookAMoved = true;
         }
      }

      System.out.println((player == 0 ? "Weiß" : "Schwarz") + " rochiert " + (kingside ? "kurz." : "lang."));
   }

   // Spielzustand nach jedem Zug prüfen 
   public static void checkGameState() {

      if (isCheckmate(board, player)) {
         System.out.println("Schachmatt! " + (player == 1 ? "Weiß" : "Schwarz") + " hat gewonnen!");
         done = true;
      } else if (isStalemate(board, player)) {
         System.out.println("Unentschieden! Patt!");
         done = true;
      } else {
         int kp = findKing(board, player);
         if (kp != -1 && isAttacked(board, kp / 8, kp % 8, player)) {
            System.out.println("Schach!");
         }

         System.out.println((player == 0 ? "Weiß" : "Schwarz") + " ist am Zug.");
      }
   }

   public static void refreshBoard() {
      chessBoard.clear();

      for (int row = 0; row < BOARD; row++) {
         for (int col = 0; col < BOARD; col++) {

            if ((row + col) % 2 == 0) {
               chessBoard.setColor(Pad.white);
            } else {
               chessBoard.setColor(Pad.lightBlue);
            }

            if (row == selectedRow && col == selectedCol) {
               chessBoard.setColor(Pad.yellow);
            }

            chessBoard.fillRect(col * SIZE, row * SIZE, SIZE, SIZE);

            int piece = board[row][col];
            if (piece != 0) {
               chessBoard.drawImage(col * SIZE, row * SIZE, SIZE, SIZE, imageFile(piece));
            }
         }
      }

      drawCoordinates();
      chessBoard.redraw();
   }

   public static String imageFile(int piece) {

      String[] names = {
         "",
         "white_pawn", "white_rook", "white_knight", "white_bishop", "white_queen", "white_king",
         "black_pawn", "black_rook", "black_knight", "black_bishop", "black_queen", "black_king"
      };
      return IMG + names[piece] + ".png";
   }

   public static void drawCoordinates() {

      chessBoard.setColor(Pad.black);

      for (int i = 0; i < 8; i++) {
         chessBoard.drawString("" + (8 - i), 5, i * SIZE + 20);
      }

      for (int i = 0; i < 8; i++) {
         chessBoard.drawString("" + (char) ('A' + i), i * SIZE + 5, SIZE * BOARD - 5);
      }
   }

   public static void fillBoard() {

      board[0][0] = 8;  board[0][7] = 8;
      board[0][1] = 9;  board[0][6] = 9;
      board[0][2] = 10; board[0][5] = 10;
      board[0][3] = 11;
      board[0][4] = 12;

      for (int i = 0; i < 8; i++){
         board[1][i] = 7;
      } 
      for (int i = 0; i < 8; i++) {
         board[6][i] = 1;
      }

      board[7][0] = 2;  board[7][7] = 2;
      board[7][1] = 3;  board[7][6] = 3;
      board[7][2] = 4;  board[7][5] = 4;
      board[7][3] = 5;
      board[7][4] = 6;
   }

   public static boolean outOfBounds(int row, int col) {
      return row < 0 || row > 7 || col < 0 || col > 7;
   }

   public static boolean isValidMove(int row_own, int col_own, int row, int col, int[][] b, int player) {

      if (outOfBounds(row, col)) return false;
      if (row_own == row && col_own == col) return false;

      int piece = b[row_own][col_own];
      int piece_enemy = b[row][col];

      if (piece_enemy > player * 6 && piece_enemy <= player * 6 + 6) return false;

      int move_x = row - row_own;
      int move_y = col - col_own;

      if (piece == 1 || piece == 7) {

         int direction;
         int start_row;

         if (piece == 1) {
            direction = -1;
            start_row = 6;
         } else {
            direction = 1;
            start_row = 1;
         }

         if (piece_enemy != 0 && move_x == direction && (move_y == 1 || move_y == -1))
            return true;

         if (piece_enemy == 0 && move_y == 0) {
            if (move_x == direction)
               return true;
            if (row_own == start_row && move_x == 2 * direction && b[row_own + direction][col_own] == 0)
               return true;
         }

         return false;

      } else if (piece == 2 || piece == 8) {
         return (move_x == 0 || move_y == 0) && freePath(b, row_own, col_own, row, col, false);

      } else if (piece == 3 || piece == 9) {
         int ax = Math.abs(move_x), ay = Math.abs(move_y);
         return (ax == 1 && ay == 2) || (ax == 2 && ay == 1);

      } else if (piece == 4 || piece == 10) {
         return Math.abs(move_x) == Math.abs(move_y) && freePath(b, row_own, col_own, row, col, true);

      } else if (piece == 5 || piece == 11) {
         if (move_x == 0 || move_y == 0) {
            return freePath(b, row_own, col_own, row, col, false);
         }
            
         if (Math.abs(move_x) == Math.abs(move_y)) {
            return freePath(b, row_own, col_own, row, col, true);
         }
         return false;

      } else if (piece == 6 || piece == 12) {
         return Math.abs(move_x) <= 1 && Math.abs(move_y) <= 1;
      }

      return false;
   }

   public static boolean freePath(int[][] b, int r0, int c0, int r1, int c1, boolean diag) {
      int sx = Integer.compare(r1 - r0, 0);
      int sy = Integer.compare(c1 - c0, 0);
      int steps;

      if (diag) {
         steps = Math.abs(r1 - r0);
      } else {
         steps = Math.max(Math.abs(r1 - r0), Math.abs(c1 - c0));
      }

      for (int i = 1; i < steps; i++) {
         if (b[r0 + i * sx][c0 + i * sy] != 0) return false;
      }

      return true;
   }

   public static int findKing(int[][] b, int player) {
      int king_value = 6 * (player + 1);

      for (int i = 0; i < 64; i++) {
         if (b[i / 8][i % 8] == king_value) return i;
      }

      return -1;
   }

   public static boolean isAttacked(int[][] b, int row, int col, int player) {

      int enemy_min = ((player + 1) % 2) * 6;
      int enemy_max = enemy_min + 6;

      for (int i = 0; i < 64; i++) {

         int r = i / 8, c = i % 8;
         int piece = b[r][c];

         if (piece > enemy_min && piece <= enemy_max) {
            if (piece == enemy_min + 6) {

               if (Math.abs(r - row) <= 1 && Math.abs(c - col) <= 1) return true;

            } else if (piece == enemy_min + 1) {

               int dir;

               if (enemy_min == 0) {
                  dir = -1;
               } else {
                  dir = 1;
               }

               int mx = row - r;
               int my = col - c;

               if (mx == dir && (my == 1 || my == -1)) return true;

            } else {

               int enemyPlayer;

               if (enemy_min == 0) {
                  enemyPlayer = 0;
               } else {
                  enemyPlayer = 1;
               }

               if (isValidMove(r, c, row, col, b, enemyPlayer)) return true;
            }
         }
      }
      return false;
   }

   public static boolean hasLegalMove(int[][] b, int player) {
      int min = player * 6, max = min + 6;

      for (int from = 0; from < 64; from++) {

         int fr = from / 8, fc = from % 8;
         int piece = b[fr][fc];

         if (piece <= min || piece > max) continue;

         for (int to = 0; to < 64; to++) {

            int tr = to / 8, tc = to % 8;
            if (fr == tr && fc == tc) continue;

            if (isValidMove(fr, fc, tr, tc, b, player)) {

               int captured = b[tr][tc], moved = b[fr][fc];
               b[tr][tc] = moved; b[fr][fc] = 0;
               int kp = findKing(b, player);
               boolean inCheck;

               if (kp == -1) {
                  inCheck = true;
               } else {
                  inCheck = isAttacked(b, kp / 8, kp % 8, player);
               }

               b[fr][fc] = moved; b[tr][tc] = captured;
               if (!inCheck) return true;
            }
         }
      }
      return false;
   }

   public static boolean isCheckmate(int[][] b, int player) {
      int kp = findKing(b, player);

      if (kp == -1) return false;

      return isAttacked(b, kp / 8, kp % 8, player) && !hasLegalMove(b, player);
   }

   public static boolean isCheckmate(int player) {
      return isCheckmate(board, player);
   }

   public static boolean isStalemate(int[][] b, int player) {
      int kp = findKing(b, player);
      if (kp == -1) return false;
      return !isAttacked(b, kp / 8, kp % 8, player) && !hasLegalMove(b, player);
   }
}