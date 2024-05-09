package pl1;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;

/*クライアントプログラムのうち画面描画に関するものである
  5/7時点でデザインについて最低限必要な機能、ボタンは実装済みのはず
  配置やデザインについては未完成 
  完成次第更新します
                                                             瀬野 拓実*/

public class client_seno extends JFrame implements ActionListener {
	private String mode; //以下の5つの文字列のいずれか 

	final String EASY = "やさしい"; //コンピュータレベルを指す 3つのいずれかならば同時にローカル対戦中である
	final String NORMAL = "ふつう";
	final String HARD = "難しい";

	final String LOCAL = "ローカル";
	final String NETWORK = "ネットワーク";

	final int BOARD_BORDER = 8;

	JButton[][] boardBottons = new JButton[BOARD_BORDER][BOARD_BORDER]; //ボタン配列 要素となるボタンはコンストラクタ内で宣言
	int map[][] = { //環境テスト用
			{ 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 1, 2, 3, 3, 0, 0 },
			{ 0, 0, 2, 4, 5, 0, 0, 0 },
			{ 0, 0, 3, 5, 4, 0, 0, 0 },
			{ 0, 0, 2, 0, 0, 1, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0 } };
	boolean isBlack = true; //プレイヤが黒ならばtrue

	Othello othello = new Othello();
	CPU cpu;
	int[] cpuPut = new int[2];

	public client_seno() {

		super("Othello");
		setResizable(true); //現時点ではフレームのサイズ変更はできない前提で書いている
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		for (int i = 0; i < 64; i++) {
			BoardButton button = new BoardButton(i); //新しく宣言したBoardButtonクラスのインスタンス
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int i = button.getI();
					int state = othello.getGridState(i / BOARD_BORDER, i % BOARD_BORDER); //Othelloクラスから呼び出しに書き換え

					if (isBlack && (state == 1 || state == 3)) {
						othello.mainboard = Othello.update(othello.getBoard(), i / BOARD_BORDER, i % BOARD_BORDER, 1);
						reflectMap();
						if (mode == NETWORK) {

						} else {
							cpu.CPUMain(othello.getBoard());
							cpuPut = cpu.getMove();
							Othello.update(othello.getBoard(), cpuPut[0], cpuPut[1], 2);
							reflectMap();
						}

					} else if (!isBlack && (state == 2 || state == 3)) {
						Othello.update(othello.getBoard(), i / BOARD_BORDER, i % BOARD_BORDER, 2);
						reflectMap();
						if (mode == NETWORK) {

						} else {
							cpu.CPUMain(othello.getBoard());
							cpuPut = cpu.getMove();
							Othello.update(othello.getBoard(), cpuPut[0], cpuPut[1], 1);
							reflectMap();
						}

					}

					if (othello.endCheck() == 1) {
						paintResult();
					}
				}
			});
			boardBottons[i / BOARD_BORDER][i % BOARD_BORDER] = button;
			button.setBackground(Color.GREEN);
			button.setBorder(new LineBorder(Color.BLACK));
		}
		chooseMode();

	}

	public void chooseMode() { //ローカル対戦かネットワーク対戦か選ばせる

		getContentPane().removeAll(); //画面のボタンやラベルをリセット
		setSize(400, 200);
		JPanel p = new JPanel();
		p.setLayout(null);

		JLabel modeLabel = new JLabel("モードを選択してください");
		modeLabel.setBounds(120, 20, 200, 30);
		p.add(modeLabel);

		JButton localButton = new JButton(LOCAL);
		localButton.setBounds(200, 100, 130, 30);
		localButton.addActionListener(this);
		p.add(localButton);

		JButton networkButton = new JButton(NETWORK);
		networkButton.setBounds(50, 100, 130, 30);
		networkButton.addActionListener(this);
		p.add(networkButton);

		add(p);
		setVisible(true);
	}

	public void setName() {

		getContentPane().removeAll(); //画面のボタンやラベルをリセット
		JPanel p = new JPanel();
		p.setLayout(new FlowLayout());

		JTextArea textArea = new JTextArea();
		textArea.setText("Player ");
		p.add(textArea);

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (textArea.getText() != null) { //空列は許さない
					Player.setPlayer_name(textArea.getText());
					//サーバに名前を送信
					paintGame(); //対局画面描画に移行 
				}
			}
		});
		p.add(okButton);

		JButton resetButton = new JButton("リセット");
		resetButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				textArea.setText("Player ");

			}
		});
		add(p);

		revalidate();
		repaint();
	}

	public void chooseLevel() {

		getContentPane().removeAll(); //画面のボタンやラベルをリセット
		JPanel p = new JPanel();
		p.setLayout(new GridBagLayout());

		JLabel titleText = new JLabel("難易度の選択");
		p.add(titleText);
		titleText.setBounds(120, 20, 200, 30);

		JLabel levelText = new JLabel("難易度を選択してください");
		p.add(levelText);
		levelText.setBounds(120, 150, 200, 30);

		JButton easyButton = new JButton(EASY);
		p.add(easyButton);
		easyButton.addActionListener(this);

		JButton normalButton = new JButton(NORMAL);
		p.add(normalButton);
		normalButton.addActionListener(this);

		JButton hardButton = new JButton(HARD);
		p.add(hardButton);
		hardButton.addActionListener(this);

		
		if(mode==LOCAL) {
			JRadioButton blackButton=new JRadioButton("先手:黒",true);
			JRadioButton whiteButton=new JRadioButton("後手:白",false);
			ButtonGroup group=new ButtonGroup();
			group.add(blackButton);
			group.add(whiteButton);
			
			p.add(blackButton);
			p.add(whiteButton);
		}
		add(p);

		revalidate();
		repaint();
	}

	public void paintGame() {
		System.out.println(isBlack);
		getContentPane().removeAll(); //画面のボタンやラベルをリセット
		setSize(700, 700);

		JPanel pMain = new JPanel();
		pMain.setLayout(new BorderLayout());
		JPanel pBoard = new JPanel();
		pBoard.setLayout(new GridLayout(BOARD_BORDER, BOARD_BORDER));
		for (int i = 0; i < 64; i++) {
			pBoard.add(boardBottons[i / BOARD_BORDER][i % BOARD_BORDER]);
		}
		pMain.add(pBoard, "Center");

		JLabel modeLabel = new JLabel();
		if (mode == NETWORK) {
			modeLabel.setText(mode);
		} else {
			modeLabel.setText("コンピュータ:" + mode);
		}

		JButton retire = new JButton("投了");
		retire.addActionListener(this);
		JButton pass = new JButton("パス");
		JLabel otherName = new JLabel("VS 対戦相手 あなたは黒です");
		//othernameのテキスト設定

		JLabel turnLabel = new JLabel("あなたの番です");
		JPanel lowerP = new JPanel();
		lowerP.add(retire);
		lowerP.add(pass);
		lowerP.add(otherName);
		lowerP.add(turnLabel);
		pMain.add(lowerP, "South");

		pMain.add(modeLabel, "North");
		add(pMain);
		setVisible(true);
		reflectMap();

	}

	public void reflectMap() {
		for (int i = 0; i < BOARD_BORDER; i++) {
			for (int j = 0; j < BOARD_BORDER; j++) {
				switch (map[i][j]) {
				case 0:
					boardBottons[i][j].setText("");
					boardBottons[i][j].setEnabled(false);
					break;
				case 1:
					boardBottons[i][j].setText("");
					boardBottons[i][j].setEnabled(isBlack);
					break;
				case 2:
					boardBottons[i][j].setText("");
					boardBottons[i][j].setEnabled(!isBlack);
					break;
				case 3:
					boardBottons[i][j].setText("");
					boardBottons[i][j].setEnabled(true);
					break;
				case 4:
					boardBottons[i][j].setText("●");
					boardBottons[i][j].setEnabled(true);
					break;
				case 5:
					boardBottons[i][j].setText("〇");
					boardBottons[i][j].setEnabled(true);
					break;
				}
			}
		}

		/*終了判定を行う otthel.endCheck()
		 * 終了している->paintResult()
		*/
		revalidate();
		repaint();
	}

	class BoardButton extends JButton { //盤面のボタンに用いる 何番目のボタンかを示す整数型変数 i を保持

		int i;

		public BoardButton(int i) {
			setI(i);
		}

		public int getI() {
			return i;
		}

		public void setI(int i) {
			this.i = i;
		}

	}

	public void paintResult() {
		othello.judge();
		othello.getBlack();
		othello.getWhite();

		setSize(400, 200);
		getContentPane().removeAll();
		System.out.println("result");
		JLabel result = new JLabel();
		result.setText("黒 33-4 白");
		add(result);
		revalidate();
		repaint();
	}

	public static void main(String[] args) {
		// TODO 自動生成されたメソッド・スタブ
		client_seno client = new client_seno();

		client.chooseMode();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		switch (e.getActionCommand()) {
		case EASY://コンピュータのレベル選び段階
		case NORMAL:
		case HARD:
			mode = e.getActionCommand(); //それぞれのレベルに設定
			cpu = new CPU(mode, "black");
			paintGame();
			break;
		case LOCAL: //モード設定段階
			mode = LOCAL;
			chooseLevel(); //コンピュータのレベル選びに移行
			break;
		case NETWORK: //モード設定段階
			mode = NETWORK;
			setName(); //プレイヤの名前設定に移行
			break;
		case "投了":
			paintResult();
			break;
		case "パス": //パスの処理
			break;
		case "先手:黒":
			isBlack=true;
			break;
		case "後手:白":
			isBlack=false;
			System.out.println("puch");
			break;
		}

	}

}
