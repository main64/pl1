package pl1;
public class Othello {
	
	/*
	 
	 主なメソッド--------------------
	 
	 Othello: コンストラクタ
	 
	 [インスタンスメソッド] (Othelloクラスのインスタンスが持つmainboard[][]を扱うメソッド)
	 putDisk: インスタンスが持つ盤面を更新するメソッド
	 pass: ターンを変更するメソッド
	 
	 [静的メソッド]
	 update: 盤面に新しい石を置いた場合の次の盤面を計算し返す汎用メソッド
	 endCheck: 盤面の終了判定メソッド
	 judge: 盤面の勝敗判定メソッド
	 
	 盤面データの扱い--------------------
	 
	 Othelloクラスで扱う盤面配列において
	 0: 空
	 1: 空(黒のみ置ける)
	 2: 空(白のみ置ける)
	 3: 空(両方置ける)
	 4: 黒
	 5: 白
	 としている
	 
	 ------------------------------
	 
	 */
	
	
	
//	クラス変数
	int[][] mainboard = {
			{0, 0, 0, 0, 0, 0, 0, 0},
			{0, 0, 0, 0, 0, 0, 0, 0},
			{0, 0, 0, 2, 1, 0, 0, 0},
			{0, 0, 2, 4, 5, 1, 0, 0},
			{0, 0, 1, 5, 4, 2, 0, 0},
			{0, 0, 0, 1, 2, 0, 0, 0},
			{0, 0, 0, 0, 0, 0, 0, 0},
			{0, 0, 0, 0, 0, 0, 0, 0}
			}; // Othelloクラスのインスタンスに対応する盤面データ
	int turn = 0; // インスタンスに対応する手番データ(0:先手/黒 1:後手/白)
	
	
	
//	------------------------------
	
	
	
//	コンストラクタ
	Othello() {}
	
//	インスタンスが持つ盤面を更新するメソッド
//	[引数] y,x: 次に石を置く座標
//	[戻り値] なし
	public void putDisc(int y, int x) {
		mainboard = update(mainboard, y, x, turn + 1);
		if (turn == 0) {
			turn = 1;
		} else {
			turn = 0;
		}
	}
	
//	ターンを変更するメソッド
//	[引数] なし
//	[戻り値] なし
	public void pass() {
		if (turn == 0) {
			turn = 1;
		} else {
			turn = 0;
		}
		return;
	}
	
//	盤面の黒の数を返すメソッド
//	[引数] なし
//	[戻り値] int:黒の数
	public int getBlack() {
		int num = 0;
		for(int i=0; i<8; i++) {
			for(int j=0; j<8; j++) {
				if(mainboard[i][j] == 4) {
					num++;
				}
			}
		}
		return num;
	}
	
//	盤面の白の数を返すメソッド
//	[引数] なし
//	[戻り値] int:白の数
	public int getWhite() {
		int num = 0;
		for(int i=0; i<8; i++) {
			for(int j=0; j<8; j++) {
				if(mainboard[i][j] == 5) {
					num++;
				}
			}
		}
		return num;
	}
	
	
	public int passCheck() {
		int flag = 0;
		for(int i=0; i<8; i++) {
			for(int j=0; j<8; j++) {
				if(mainboard[i][j] >= 1 && mainboard[i][j] <= 3) {
					flag = 1;
				}
			}
		}
		if (flag == 0) {
			return 1;
		} else {
			return 0;
		}
	}
	
//	盤面全体をを配列として返すメソッド
//	[引数] なし
//	[戻り値] int[][]:盤面
	public int[][] getBoard() {
		return mainboard;
	}
	
//	盤面全体をを配列として返すメソッド
//	[引数] なし
//	[戻り値] int[][]:盤面
	public int getGridState(int y, int x) {
		return mainboard[y][x];
	}
	
//	盤面の勝敗判定メソッド
//	[引数] なし
//	[戻り値] String:勝者("draw"/"black"/"white")
	public String judge() {
		int black = 0, white = 0;
		for(int i=0; i<8; i++) {
			for(int j=0; j<8; j++) {
				if(mainboard[i][j] == 4) {
					black++;
				}
				if(mainboard[i][j] == 5) {
					white++;
				}
			}
		}
		if (black == white) {
			return "draw";
		} else if (black > white) {
			return "black";
		} else {
			return "white";
		}
	}
	
//	盤面の終了判定メソッド
//	[引数] なし
//	[戻り値] int:(0:非終了 1:終了)
	public int endCheck() {
		int flag = 0;
		for(int i=0; i<8; i++) {
			for(int j=0; j<8; j++) {
				if(mainboard[i][j] >= 1 && mainboard[i][j] <= 3) {
					flag = 1;
				}
			}
		}
		
		if (flag == 1) {
			return 0;
		} else {
			return 1;
		}
	}
	
	
	
//	------------------------------
	
	
	
//	盤面に新しい石を置いた場合の次の盤面を計算し返す汎用メソッド
//	[引数] board[8][8]:盤面 | y,x:石を置く座標 | color:置く石の色(1:黒 2:白)
//	[戻り値] int[8][8]:更新後の盤面
	static public int[][] update(int board[][], int y, int x, int color) {
		
		board[y][x] = color + 3;
		
//		空のマスを0で統一
		for(int i=0; i<8; i++) {
			for(int j=0; j<8; j++) {
				if(board[i][j] <= 3) {
					board[i][j] = 0;
				}
			}
		}
		
//		ひっくり返す
		board = reverse(board, y, x, 1, 0, color);
		board = reverse(board, y, x, -1, 0, color);
		board = reverse(board, y, x, 0, 1, color);
		board = reverse(board, y, x, 0, -1, color);
		board = reverse(board, y, x, 1, 1, color);
		board = reverse(board, y, x, 1, -1, color);
		board = reverse(board, y, x, -1, -1, color);
		board = reverse(board, y, x, -1, 1, color);
		
//		空のマスから置けるところを探索して更新
		for(int i=0; i<8; i++) {
			for(int j=0; j<8; j++) {
				if(board[i][j] <= 3) {
//					黒
					int flag = 0;
					if(check(board, i, j, 1, 0, 1) == 1) {flag = 1;}
					if(check(board, i, j, -1, 0, 1) == 1) {flag = 1;}
					if(check(board, i, j, 0, 1, 1) == 1) {flag = 1;}
					if(check(board, i, j, 0, -1, 1) == 1) {flag = 1;}
					if(check(board, i, j, 1, 1, 1) == 1) {flag = 1;}
					if(check(board, i, j, 1, -1, 1) == 1) {flag = 1;}
					if(check(board, i, j, -1, -1, 1) == 1) {flag = 1;}
					if(check(board, i, j, -1, 1, 1) == 1) {flag = 1;}
					if (flag == 1) {
						board[i][j] += 1;
					}
					
//					白
					flag = 0;
					if(check(board, i, j, 1, 0, 2) == 1) {flag = 1;}
					if(check(board, i, j, -1, 0, 2) == 1) {flag = 1;}
					if(check(board, i, j, 0, 1, 2) == 1) {flag = 1;}
					if(check(board, i, j, 0, -1, 2) == 1) {flag = 1;}
					if(check(board, i, j, 1, 1, 2) == 1) {flag = 1;}
					if(check(board, i, j, 1, -1, 2) == 1) {flag = 1;}
					if(check(board, i, j, -1, -1, 2) == 1) {flag = 1;}
					if(check(board, i, j, -1, 1, 2) == 1) {flag = 1;}
					if (flag == 1) {
						board[i][j] += 2;
					}
				}
			}
		}
		return board;
	}
	
	
	
//	------------------------------
	
	
	
//	ひっくり返るコマを計算するメソッド(updateからのみ使用)
	static private int[][] reverse(int board[][], int y, int x, int dy, int dx, int color) {
		int py = y;
		int px = x;
		int flag = 0;
		
		if (color == 1) {
			px += dx;
			py += dy;
			if (0 <= px && px <= 7 && 0 <= py && py <= 7) {
				while(board[py][px] == 5) {
					flag = 1;
					px += dx;
					py += dy;
					if (0 > px || px > 7 || 0 > py || py > 7) {return board;}
				}
			}
			if (flag == 1 && board[py][px] == 4) {
				flag = 2;
			}
			if (flag == 2) {
				py = y;
				px = x;
				px += dx;
				py += dy;
				while(board[py][px] == 5) {
					board[py][px] = 4;
					px += dx;
					py += dy;
				}
			}
		}
		if (color == 2) {
			px += dx;
			py += dy;
			if (0 <= px && px <= 7 && 0 <= py && py <= 7) {
				while(board[py][px] == 4) {
					flag = 1;
					px += dx;
					py += dy;
					if (0 > px || px > 7 || 0 > py || py > 7) {return board;}
				}
			}
			if (flag == 1 && board[py][px] == 5) {
				flag = 2;
			}
			if (flag == 2) {
				py = y;
				px = x;
				px += dx;
				py += dy;
				while(board[py][px] == 4) {
					board[py][px] = 5;
					px += dx;
					py += dy;
				}
			}
		}
		return board;
	}

//	任意のマスに任意の色の石が置けるか確かめるメソッド(updateからのみ使用)
	static private int check(int board[][], int y, int x, int dy, int dx, int color) {
		int py = y;
		int px = x;
		int flag = 0;
		
		if (color == 1) {
			px += dx;
			py += dy;
			
			if (0 <= px && px <= 7 && 0 <= py && py <= 7) {
				while(board[py][px] == 5) {
					flag = 1;
					px += dx;
					py += dy;
					if (0 > px || px > 7 || 0 > py || py > 7) {return 0;}
				}
			}
			if (flag == 1 && board[py][px] == 4 && 0 <= px && px <= 7 && 0 <= py && py <= 7) {
				return 1;
			}
		}
		if (color == 2) {
			px += dx;
			py += dy;
			
			if (0 <= px && px <= 7 && 0 <= py && py <= 7) {
				while(board[py][px] == 4) {
					flag = 1;
					px += dx;
					py += dy;
					if (0 > px || px > 7 || 0 > py || py > 7) {return 0;}
				}
			}
			if (flag == 1 && board[py][px] == 5 && 0 <= px && px <= 7 && 0 <= py && py <= 7) {
				return 1;
			}
		}
		return 0;
	}
	
//	盤面をコンソールに描画するメソッド(テスト用)
	public void printBoard() {
		for(int i=0; i<8; i++) {
			for(int j=0; j<8; j++) {
				System.out.print(mainboard[i][j] + " ");
			}
			System.out.print("   ");
			for(int j=0; j<8; j++) {
				switch (mainboard[i][j]) {
					case 4:
						System.out.print("● ");
						break;
					case 5:
						System.out.print("○ ");
						break;
					default:
						System.out.print("• ");
				}
			}
			System.out.print("\n");
		}
		System.out.print("\n");
		return;
	}
}
