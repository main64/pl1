package pl1;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class CPU {
	private String difficulty;	//難易度
	private int[][] wb = new int[8][8];	//重み付け盤面
	private String color;	//先手後手情報
	private int[] move = new int[2];	//指し手情報
	
	/*コンストラクタ*/
	public CPU(String difficulty, String PlayerColor){
		setDifficulty(difficulty);
		setCPUColor(PlayerColor);
		/*指し手情報を初期化(パスの判別ができるように)*/
	}
	
	/*難易度をセット*/
	private void setDifficulty(String difficulty) {
		this.difficulty = difficulty;
	}
	
	/*難易度を取得*/
	public String getDifficulty() {
		return difficulty;
	}
	
	/*先手後手情報をセット*/
	private void setCPUColor(String PlayerColor) {
		try {
			if(PlayerColor == "white") {
				this.color = "black";
			}else if(PlayerColor == "black") {
				this.color = "white";
			}else {
				throw new ColorException("先手後手情報を設定できません");
			}
		}catch(ColorException ce){
			System.out.println(ce.getMessage());
		}
	}
	
	/*先手後手情報を取得*/
	public String getCPUColor() {
		return color;
	}
	
	/*盤面評価関数の設定*/
	public void evaluate_init() {
		int i=0, j=0;
		
		/*csvファイルから評価値を読み取って2次元配列wbに格納*/
		/*csvファイルには盤面の左上1/4部分のみ記載*/
		try {
			File f = new File("Othello_wb.csv");
			BufferedReader br = new BufferedReader(new FileReader(f));
			String s;
			while((s = br.readLine()) != null) {
				j = 0;
				String[] data = s.split(",");
				for(String elem : data) {
					wb[i][j] = Integer.parseInt(elem);
					j++;
				}
				i++;
			}
			br.close();
		}catch(IOException e) {
			System.out.println(e);
		}
		
		/*csvファイルから読み取った盤面を反転させて8×8のwbを完成させる*/
		for(i=0; i<4; i++) {
			for(j=4; j<8; j++) {
				wb[i][j] = wb[i][7-j];
			}
		}
		for(i=4; i<8; i++) {
			for(j=0; j<4; j++) {
				wb[i][j] = wb[7-i][j];
			}
		}
		for(i=4; i<8; i++) {
			for(j=4; j<8; j++) {
				wb[i][j] = wb[7-i][7-j];
			}
		}
	}
	
	/*評価値の計算*/
	private int eval(int[][] sb) {
		int e = 0;	//評価値
		int c;	//CPUが黒ならば4, 白ならば5が入る
		
		/*黒と白どちらの評価値を求めるかを決める*/
		if(getCPUColor() == "black") {
			c = 4;
		}else {
			c = 5;
		}
		
		/*cの値に対応する方の評価値を求める*/
		for(int i=0; i<sb.length; i++) {
			for(int j=0; j<sb.length; j++) {
				if(sb[i][j] == c) {
					e = e + wb[i][j];
				}
			}
		}
		
		return e;
	}
	
	/*αβ法による探索
	  depth:現在の深さ, maxdepth:最大の深さ, sb:探索用仮盤面, c:色
	  fr:難易度easyかそれ以外か(easy:1, それ以外:0)
	  alpha: 自分が取りうる評価値の最小, beta: 相手が取りうる評価値の最大
	 */
	private int search(int depth, int maxdepth, int[][] sb, int c, int fr, int alpha, int beta) {		
		/*葉ならば評価値を返す*/
		if(depth == 0) {
			return eval(sb);
		}
		
		int[][] fb = new int[8][8];	//探索の際に利用するコピー先盤面
				
		/*再帰アルゴリズムによるαβ探索*/
		for(int i=0; i<sb.length; i++) {
			for(int j=0; j<sb.length; j++) {
				/*置ける場所が見つかった場合、探索*/
				if(sb[i][j] == c | sb[i][j] == 3) {
					/*探索用盤面に現在の盤面状況をコピー*/
					for(int k=0; k<sb.length; k++) {
						fb[i] = sb[i].clone();
					}
					/*
					 *Othelloクラスのメソッドを使ってfb[i][j]に置いた
					 *時の局面の更新を行う処理をここに書く
					 */
					int value = search(depth-1, maxdepth, fb, 3-c, fr, alpha, beta);	//再帰呼び出し
					if((maxdepth - depth) % 2 == 1) {	//相手側が選択する手を考えるとき
						if(value < beta) {
							beta = value;	//相手は評価値が低くなる手を取る
						}
						if(fr == 0) {
							if(alpha >= beta) {	//今のノードの評価値は兄弟ノードよりも低くなることが確定している
								return beta;	//αカット
							}
						}
					}else {	//自分側が選択する手を考えるとき
						if(fr == 0) {	//難易度normal以上の時
							if(value > alpha) {
								alpha = value;	//自分は評価値が高くなる手を取る
								/*もしゲーム木の根にいる場合、暫定の指し手を保存*/
								if(depth == maxdepth) {
									move[0] = i;
									move[1] = j;
								}
							}
							if(alpha >= beta) {	//今のノードの評価値は兄弟ノードよりも高くなることが確定している
								return alpha;	//βカット
							}
						}else {	//難易度easyの時
							if(value < alpha) {
								alpha = value;	//自分も評価値が低くなる手を取る
								/*もしゲーム木の根にいる場合、暫定の指し手を保存*/
								if(depth == maxdepth) {
									move[0] = i;
									move[1] = j;
								}
							}
						}
					}
				}
			}
		}
		
		/*
		 *ここにパスの時の処理を書く 
		 */
		
		/*自分の手番ならα値, 相手の手番ならβ値を返す*/
		if((maxdepth - depth) % 2 == 1) {
			return beta;
		}else {
			return alpha;
		}
	}
	
	/*難易度易*/
	private void easy(int[][] board, int c) {
		search(6, 6, board, c, 1, Integer.MAX_VALUE, Integer.MAX_VALUE);
	}
	
	/*難易度中*/
	private void normal(int[][] board, int c) {
		search(2, 2, board, c, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
		
	}
	
	/*難易度高*/
	private void difficult(int[][] board, int c) {
		search(6, 6, board, c, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}
	
	/*CPUクラスのメイン関数のようなもの*/
	public void CPUMain(int[][] board) {	
		int c;	//CPUが黒ならば1, CPUが白ならば2が入る
		
		/*cに入れる値を決める*/
		if(getCPUColor() == "black") {
			c = 1;
		}else {
			c = 2;
		}
		
		/*探索方法を場合分け*/
		try{
			if(difficulty == "易しい") {
				easy(board, c);
			}else if(difficulty == "普通") {
				normal(board, c);
			}else if(difficulty == "難しい") {
				difficult(board, c);
			}else {
				throw new DifficultyException("難易度が正しくありません");
			}
		}catch(DifficultyException de) {
			System.out.println(de.getMessage());
		}		
	}
	
	/*指し手を取得*/
	public int[] getMove() {
		return move;
	}
}


/*難易度が正しく設定されていないときの例外クラス */
class DifficultyException extends Exception{
	DifficultyException(String msg){
		super(msg);
	}
}


/*先手情報が正しく設定できないときの例外クラス*/
class ColorException extends Exception{
	ColorException(String msg){
		super(msg);
	}
}