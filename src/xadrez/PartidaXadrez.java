package xadrez;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import tabuleiro.Peca;
import tabuleiro.Posicao;
import tabuleiro.Tabuleiro;
import xadrez.pecas.Bispo;
import xadrez.pecas.Cavalo;
import xadrez.pecas.Peao;
import xadrez.pecas.Rainha;
import xadrez.pecas.Rei;
import xadrez.pecas.Torre;

public class PartidaXadrez {

	private int turno;
	private Cor jogadorAtual;
	private Tabuleiro tabuleiro;
	private boolean check;
	private boolean checkMate;
	private PecaXadrez possivelEnPassant;
	private PecaXadrez promocao;
	
	private List<Peca> pecasNoTabuleiro = new ArrayList<>();
	private List<Peca> pecasCapturadas = new ArrayList<>();
	
	public PartidaXadrez() {
		tabuleiro = new Tabuleiro(8, 8);
		turno = 1;
		jogadorAtual = Cor.BRANCO;
		iniciarPartida();
	}

	public int getTurno() {
		return turno;
	}

	public Cor getJogadorAtual() {
		return jogadorAtual;
	}
	
	public boolean getCheck() {
		return check;
	}
	
	public boolean getCheckMate() {
		return checkMate;
	}
	
	public PecaXadrez getPossivelEnPassant() {
		return possivelEnPassant;
	}
	
	public PecaXadrez getPromocao() {
		return promocao;
	}

	public PecaXadrez[][] getPecas() {
		PecaXadrez[][] matriz = new PecaXadrez[tabuleiro.getLinhas()][tabuleiro.getColunas()];
		for (int i = 0; i < tabuleiro.getLinhas(); i++) {
			for (int j = 0; j < tabuleiro.getColunas(); j++) {
				matriz[i][j] = (PecaXadrez) tabuleiro.peca(i, j);
			}
		}
		return matriz;
	}

	public boolean[][] possiveisMovimentos(PosicaoXadrez posicaoDeOrigem){
		Posicao posicao = posicaoDeOrigem.toPosicao();
		validarPosicaoOrigem(posicao);
		return tabuleiro.peca(posicao).possiveisMovimentos();
	}
	
	public PecaXadrez executarPecaXadrez(PosicaoXadrez posicaoOrigem, PosicaoXadrez posicaoDestino) {
		Posicao origem = posicaoOrigem.toPosicao();
		Posicao destino = posicaoDestino.toPosicao();
		validarPosicaoOrigem(origem);
		validarPosicaoDestino(origem, destino);
		Peca pecaCapturada = mover(origem, destino);
		
		if(testarCheck(jogadorAtual)) {
			desfazerMovimento(origem, destino, pecaCapturada);
			throw new ExecaoXadrez("Voce nao pode se colocar em check.");
		}
		
		PecaXadrez pecaMovida = (PecaXadrez) tabuleiro.peca(destino);
		
		//Movimento especial Promocao
		promocao = null;
		if(pecaMovida instanceof Peao) {
			if((pecaMovida.getCor() == Cor.BRANCO && destino.getLinha() == 0 || pecaMovida.getCor() == Cor.PRETO && destino.getLinha() == 7)) {
				promocao = (PecaXadrez) tabuleiro.peca(destino);
				promocao = pecaPromovida("Q");
			}
		}
		
		check = (testarCheck(oponente(jogadorAtual))) ? true : false;
		
		if(testarCheckMate(oponente(jogadorAtual))) {
			checkMate = true;
		}
		else {
			proximoTurno();
		}
		
		//Movimento especial En Passant
		if(pecaMovida instanceof Peao && destino.getLinha() == origem.getLinha() - 2 || destino.getLinha() == origem.getLinha() + 2) {
			possivelEnPassant = pecaMovida;
		}else {
			possivelEnPassant = null;
		}
		
		return (PecaXadrez) pecaCapturada;
	}
	
	public PecaXadrez pecaPromovida(String tipo) {
		if(promocao == null) {
			throw new IllegalStateException("Nao ha peca para ser promovida.");
		}
		if(!tipo.equals("B") && !tipo.equals("T") && !tipo.equals("C") && !tipo.equals("Q")) {
			return promocao;
		}
		
		Posicao pos = promocao.getPosicaoXadrez().toPosicao();
		Peca p = tabuleiro.removerPeca(pos);
		pecasNoTabuleiro.remove(p);
		
		PecaXadrez novaPeca = novaPeca(tipo, promocao.getCor());
		tabuleiro.lugarPeca(novaPeca, pos);
		pecasNoTabuleiro.add(novaPeca);
		
		return novaPeca;
	}
	
	private PecaXadrez novaPeca(String tipo, Cor cor) {
		if(tipo.equals("B")) return new Bispo(tabuleiro, cor);
		if(tipo.equals("T")) return new Torre(tabuleiro, cor);
		if(tipo.equals("C")) return new Cavalo(tabuleiro, cor);
		return new Rainha(tabuleiro, cor);
	}
	
	private Peca mover(Posicao origem, Posicao destino) {
		PecaXadrez p = (PecaXadrez)tabuleiro.removerPeca(origem);
		p.incrementarContador();
		Peca pecaCapturada = tabuleiro.removerPeca(destino);
		tabuleiro.lugarPeca(p, destino);
		
		if(pecaCapturada != null) {
			pecasNoTabuleiro.remove(pecaCapturada);
			pecasCapturadas.add(pecaCapturada);
		}
		
		//"Roque pequeno"
		if(p instanceof Rei && destino.getColuna() == origem.getColuna() + 2) {
			Posicao origemTorre = new Posicao(origem.getLinha(), origem.getColuna() + 3);
			Posicao destinoTorre = new Posicao(origem.getLinha(), origem.getColuna() + 1);
			PecaXadrez torre = (PecaXadrez) tabuleiro.removerPeca(origemTorre);
			tabuleiro.lugarPeca(torre, destinoTorre);
			torre.incrementarContador();
		}
		
		//"Roque grande"
		if(p instanceof Rei && destino.getColuna() == origem.getColuna() - 2) {
			Posicao origemTorre = new Posicao(origem.getLinha(), origem.getColuna() - 4);
			Posicao destinoTorre = new Posicao(origem.getLinha(), origem.getColuna() - 1);
			PecaXadrez torre = (PecaXadrez) tabuleiro.removerPeca(origemTorre);
			tabuleiro.lugarPeca(torre, destinoTorre);
			torre.incrementarContador();
		}
		
		//Movimento especial En Passant
		if(p instanceof Peao) {
			if(origem.getColuna() != destino.getColuna() && pecaCapturada == null) {
				Posicao posicaoPeao;
				if(p.getCor() == Cor.BRANCO) {
					posicaoPeao = new Posicao(destino.getLinha() + 1, destino.getColuna());
				}else {
					posicaoPeao = new Posicao(destino.getLinha() - 1, destino.getColuna());
				}
				pecaCapturada = tabuleiro.removerPeca(posicaoPeao);
				pecasCapturadas.add(pecaCapturada);
				pecasNoTabuleiro.remove(pecaCapturada);
			}
		}
		return pecaCapturada;
	}
	
	private void desfazerMovimento(Posicao origem, Posicao destino, Peca pecaCapturada) {
		PecaXadrez p = (PecaXadrez)tabuleiro.removerPeca(destino);
		p.decrementarContador();
		tabuleiro.lugarPeca(p, origem);
		
		if(pecaCapturada != null) {
			tabuleiro.lugarPeca(pecaCapturada, destino);
			pecasCapturadas.remove(pecaCapturada);
			pecasNoTabuleiro.add(pecaCapturada);
		}
		
		//"Roque pequeno"
		if(p instanceof Rei && destino.getColuna() == origem.getColuna() + 2) {
			Posicao origemTorre = new Posicao(origem.getLinha(), origem.getColuna() + 3);
			Posicao destinoTorre = new Posicao(origem.getLinha(), origem.getColuna() + 1);
			PecaXadrez torre = (PecaXadrez) tabuleiro.removerPeca(destinoTorre);
			tabuleiro.lugarPeca(torre, origemTorre);
			torre.decrementarContador();
		}
		
		//"Roque grande"
		if(p instanceof Rei && destino.getColuna() == origem.getColuna() - 2) {
			Posicao origemTorre = new Posicao(origem.getLinha(), origem.getColuna() - 4);
			Posicao destinoTorre = new Posicao(origem.getLinha(), origem.getColuna() - 1);
			PecaXadrez torre = (PecaXadrez) tabuleiro.removerPeca(destinoTorre);
			tabuleiro.lugarPeca(torre, origemTorre);
			torre.decrementarContador();
		}
		
		//Movimento especial En Passant
		if(p instanceof Peao) {
			if(origem.getColuna() != destino.getColuna() && pecaCapturada == possivelEnPassant) {
				PecaXadrez peao = (PecaXadrez) tabuleiro.removerPeca(destino);
				Posicao posicaoPeao;
				if(p.getCor() == Cor.BRANCO) {
					posicaoPeao = new Posicao(3, destino.getColuna());
				}else {
					posicaoPeao = new Posicao(4, destino.getColuna());
				}
				tabuleiro.lugarPeca(peao, posicaoPeao);
			}
		}
	}
	
	private void validarPosicaoOrigem(Posicao posicao) {
		if(!tabuleiro.temPeca(posicao)) {
			throw new ExecaoXadrez("Nao existe peca na posicao de origem.");
		}
		if(jogadorAtual != ((PecaXadrez) tabuleiro.peca(posicao)).getCor()) {
			throw new ExecaoXadrez("Essa peca nao eh sua.");
		}
		if(!tabuleiro.peca(posicao).umPossivelMovimento()) {
			throw new ExecaoXadrez("Nao existe movimento possivel para a peca escolhida.");
		}
	}
	
	private void validarPosicaoDestino(Posicao origem, Posicao destino) {
		if(!tabuleiro.peca(origem).possivelMovimento(destino)) {
			throw new ExecaoXadrez("A peca escolhida nao pode se mover para a posicao de destino.");
		}
	}
	
	private void proximoTurno() {
		turno++;
		jogadorAtual = (jogadorAtual == Cor.BRANCO) ? Cor.PRETO : Cor.BRANCO;
	}
	
	private Cor oponente(Cor cor) {
		return (cor == Cor.BRANCO) ? Cor.PRETO : Cor.BRANCO;
	}
	
	private PecaXadrez rei(Cor cor) {
		List<Peca> list = pecasNoTabuleiro.stream().filter(x -> ((PecaXadrez)x).getCor() == cor).collect(Collectors.toList());
		for(Peca p : list) {
			if(p instanceof Rei) {
				return (PecaXadrez)p;
			}
		}
		throw new IllegalStateException("Nao existe um rei " + cor + " no tabuleiro.");
	}
	
	private boolean testarCheck(Cor cor) {
		Posicao reiPosicao = rei(cor).getPosicaoXadrez().toPosicao();
		List<Peca> pecasOponente = pecasNoTabuleiro.stream().filter(x -> ((PecaXadrez)x).getCor() == oponente(cor)).collect(Collectors.toList());
		for(Peca p : pecasOponente) {
			boolean mat[][] = p.possiveisMovimentos();
			if(mat[reiPosicao.getLinha()][reiPosicao.getColuna()]) {
				return true;
			}
		}
		return false;
	}
	
	private boolean testarCheckMate(Cor cor) {
		if(!testarCheck(cor)) {
			return false;
		}
		List<Peca> list = pecasNoTabuleiro.stream().filter(x -> ((PecaXadrez)x).getCor() == cor).collect(Collectors.toList());
		for(Peca p : list) {
			boolean mat[][] = p.possiveisMovimentos();
			for(int i = 0; i < tabuleiro.getLinhas(); i++) {
				for(int j = 0;  j < tabuleiro.getColunas(); j++) {
					if(mat[i][j]) {
						Posicao origem = ((PecaXadrez)p).getPosicaoXadrez().toPosicao();
						Posicao destino = new Posicao(i, j);
						Peca pecaCapturada = mover(origem, destino);
						boolean testarCheck = testarCheck(cor);
						desfazerMovimento(origem, destino, pecaCapturada);
						if(!testarCheck) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}
	
	private void novoLugarPeca(char coluna, int linha, PecaXadrez peca) {
		tabuleiro.lugarPeca(peca, new PosicaoXadrez(coluna, linha).toPosicao());
		pecasNoTabuleiro.add(peca);
	}

	private void iniciarPartida() {
		novoLugarPeca('a', 1, new Torre(tabuleiro, Cor.BRANCO));
		novoLugarPeca('b', 1, new Cavalo(tabuleiro, Cor.BRANCO));
		novoLugarPeca('c', 1, new Bispo(tabuleiro, Cor.BRANCO));
		novoLugarPeca('d', 1, new Rainha(tabuleiro, Cor.BRANCO));
		novoLugarPeca('e', 1, new Rei(tabuleiro, Cor.BRANCO, this));
		novoLugarPeca('f', 1, new Bispo(tabuleiro, Cor.BRANCO));
		novoLugarPeca('g', 1, new Cavalo(tabuleiro, Cor.BRANCO));
		novoLugarPeca('h', 1, new Torre(tabuleiro, Cor.BRANCO));
		novoLugarPeca('a', 2, new Peao(tabuleiro, Cor.BRANCO, this));
		novoLugarPeca('b', 2, new Peao(tabuleiro, Cor.BRANCO, this));
		novoLugarPeca('c', 2, new Peao(tabuleiro, Cor.BRANCO, this));
		novoLugarPeca('d', 2, new Peao(tabuleiro, Cor.BRANCO, this));
		novoLugarPeca('e', 2, new Peao(tabuleiro, Cor.BRANCO, this));
		novoLugarPeca('f', 2, new Peao(tabuleiro, Cor.BRANCO, this));
		novoLugarPeca('g', 2, new Peao(tabuleiro, Cor.BRANCO, this));
		novoLugarPeca('h', 2, new Peao(tabuleiro, Cor.BRANCO, this));
		
		novoLugarPeca('a', 8, new Torre(tabuleiro, Cor.PRETO));
		novoLugarPeca('b', 8, new Cavalo(tabuleiro, Cor.PRETO));
		novoLugarPeca('c', 8, new Bispo(tabuleiro, Cor.PRETO));
		novoLugarPeca('d', 8, new Rainha(tabuleiro, Cor.PRETO));
		novoLugarPeca('e', 8, new Rei(tabuleiro, Cor.PRETO, this));
		novoLugarPeca('f', 8, new Bispo(tabuleiro, Cor.PRETO));
		novoLugarPeca('g', 8, new Cavalo(tabuleiro, Cor.PRETO));
		novoLugarPeca('h', 8, new Torre(tabuleiro, Cor.PRETO));
		novoLugarPeca('a', 7, new Peao(tabuleiro, Cor.PRETO, this));
		novoLugarPeca('b', 7, new Peao(tabuleiro, Cor.PRETO, this));
		novoLugarPeca('c', 7, new Peao(tabuleiro, Cor.PRETO, this));
		novoLugarPeca('d', 7, new Peao(tabuleiro, Cor.PRETO, this));
		novoLugarPeca('e', 7, new Peao(tabuleiro, Cor.PRETO, this));
		novoLugarPeca('f', 7, new Peao(tabuleiro, Cor.PRETO, this));
		novoLugarPeca('g', 7, new Peao(tabuleiro, Cor.PRETO, this));
		novoLugarPeca('h', 7, new Peao(tabuleiro, Cor.PRETO, this));
	}
}
