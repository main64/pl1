package pl1;
import java.util.Scanner;

public class CPUTest {
	public static void main(String[] args) {
		Othello board = new Othello();
		CPU cp = new CPU("ふつう", "black");
		int x, y;
		int[] cpu = new int[2];
		board.printBoard();
		
		Scanner stdin = new Scanner(System.in);
		
		while (board.endCheck() != 1) {
			if (board.passCheck() == 0) {
				do {
					y = -1;
					x = -1;
					while(x < 0 || x > 7 || y < 0 || y > 7) {
						System.out.print("Y: ");
						y = Integer.parseInt(stdin.next());
						System.out.print("X: ");
						x = Integer.parseInt(stdin.next());
					}
				} while (board.getGridState(y, x) != 1 && board.getGridState(y, x) != 3);
				board.putDisc(y, x);
			} else {
				System.out.print("YOU PASS!\n");
				board.pass();
			}
			
			board.printBoard();
			
			if (board.passCheck() == 0) {
				cp.CPUMain(board.mainboard);
				cpu = cp.getMove();
				System.out.print("CPU: " + cpu[0] + ", " + cpu[1] + "\n");
				board.putDisc(cpu[0], cpu[1]);
			} else {
				System.out.print("CPU PASS!\n");
			}
			
			board.printBoard();
		}
		
	}
}