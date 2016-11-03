package interfacegraphique;

import gui.GUISimulator;
import gui.ImageElement;
import gui.Simulable;
import java.io.IOException;
import carte.*;
import donneesSimulation.DonneesSimulation;
import java.util.*;
import evenements.*;
import exceptions.ErreurPosition;
import robots.*;
import java.lang.Math;

public class Simulateur implements Simulable {

    //L'interface graphique associée
    private GUISimulator gui;

    //Les données de la simuluation (carte, robots, incendies)
    private DonneesSimulation data;
    
    //Le gestionnaire d'évenements
    private GestionnaireEvents GE;
    
    //La taille de cases à l'échelle du simulateur
    private int tailleCases;
    
    private int [][] coordImageRobot;
    
    private int indiceImage = 0;
    
    /**Tout est ce qui est nécessaire au restart **/
    private int [][] savePosRobots;
    private int [] saveIntensiteIncendies;
    private GestionnaireEvents GEdebut;
    
    
    public Simulateur(GUISimulator gui, DonneesSimulation data, GestionnaireEvents GE) {
        //On instancie les attributs
        this.gui = gui;
        gui.setSimulable(this);
        this.data = data;
        Carte carte = data.getCarte();
        
        this.GE = GE;
        
        //On sauvegarde pour pouvoir restart plus tard
        save();
        
        coordImageRobot = new int[data.getListeRobots().size()][2];
        
        //On détermine la nouvelle tailleCases
        int dimFenX = gui.getPanelWidth();
        int dimFenY = gui.getPanelHeight();
        if (carte.getTailleCases() * carte.getNbLignes() > dimFenX || carte.getTailleCases() * carte.getNbColonnes() > dimFenY) {
            //pour adapter l'échelle de la carte à la taille de la fenetre graphique
            int minDim = (dimFenX > dimFenY ? dimFenY : dimFenX);
            tailleCases = (minDim == dimFenX ? minDim / carte.getNbLignes() : minDim / carte.getNbColonnes());
        } else {
            tailleCases = carte.getTailleCases();
        }
        

        gui.reset();
        
        //On dessine la carte
        drawCarte();

        //On dessine les robots
        drawListRobots();

        //On dessine les incencies
        drawListIncendies();
        
    }
    
    private void save() {
    	List<Robot> listeRobots = data.getListeRobots();
    	List<Incendie> listeIncendies = data.getListeIncendies();
     	savePosRobots = new int [2][listeRobots.size()];
    	saveIntensiteIncendies = new int [listeIncendies.size()];
    	
    	//On sauvegarde la position des robots
    	for(int i = 0; i < listeRobots.size(); i++){
    		savePosRobots[0][i] = listeRobots.get(i).getPosition().getLigne();
    		savePosRobots[1][i] = listeRobots.get(i).getPosition().getColonne();
    	}
    	
    	//On sauvegarde les intensités des incendies
    	for(int i = 0; i < listeIncendies.size(); i++) {
    		saveIntensiteIncendies[i] = listeIncendies.get(i).getNbLitres();
    	}
    	
    	//On copie la liste d'évenements
    	this.GEdebut = GE.clone();
    }
    
    public int getTailleCase() {
        return this.tailleCases;
    }

    

    @Override
    public void next() {
    	if(!GE.simulationTerminee()){
	        gui.reset();
	        GE.incrementeDate();
	        drawCarte();
	        refreshIncendies();
            try {
                refreshRobots();
            } catch (ErreurPosition ex) {
                System.out.println("Un robot est sorti de la carte. Arrêt prématuré de la simulation.");
                restart();
            }
    	}
    	else
    		System.out.println("Simulation terminée.");
    }

    @Override
    public void restart() {
    	//On remet les robots à leur état initial
    	List<Robot> listeRobots = data.getListeRobots();
    	Robot rob;
    	for(int i = 0; i < listeRobots.size(); i++) {
    		rob = listeRobots.get(i);
    		try {
    			rob.setPosition(data.getCarte().getCase(savePosRobots[0][i], savePosRobots[1][i]));
    		} catch(ErreurPosition ep) {
    			System.out.println(ep);
    		}
    		rob.setDirection(null);
    		rob.switchAction(Action.INNOCUPE);
    		rob.setVolumeRestant(rob.getCapaciteMax());
    	}
    	
    	//On remet les incendies à leur état initial
    	List<Incendie> listeIncendies = data.getListeIncendies();
    	
    	for(int i = 0; i < listeIncendies.size(); i++) {
    		listeIncendies.get(i).setNbLitres(saveIntensiteIncendies[i]);
    	}
    	
    	//On clone la suite des evenements
    	GE = GEdebut.clone();
    	gui.reset();
    	drawCarte();
        drawListRobots();
        drawListIncendies();
    }

    /**
     * Partie Dessin
     *
     * @throws IOException *
     */
    private void drawCase(int x, int y, NatureTerrain nature) {
        String pathImage = null;
        ImageElement image;

        switch (nature) {
            case EAU:
                pathImage = "images/water.png";
                image = new ImageElement(x, y, pathImage, tailleCases + 1, tailleCases + 1, null);
                gui.addGraphicalElement(image);
                break;
            case FORET:
                pathImage = "images/herb.png";
                image = new ImageElement(x, y, pathImage, tailleCases + 1, tailleCases + 1, null);
                gui.addGraphicalElement(image);
                pathImage = "images/tree.png";
                image = new ImageElement(x, y - tailleCases, pathImage, tailleCases, (int) 2.5 * tailleCases, null);
                gui.addGraphicalElement(image);
                break;
            case ROCHE:
                pathImage = "images/cherrygrove.png";
                image = new ImageElement(x, y, pathImage, tailleCases + 1, tailleCases + 1, null);
                gui.addGraphicalElement(image);
                pathImage = "images/rock.png";
                image = new ImageElement(x, y, pathImage, tailleCases, tailleCases, null);
                gui.addGraphicalElement(image);
                break;
            case TERRAIN_LIBRE:
                pathImage = "images/cherrygrove.png";
                image = new ImageElement(x, y, pathImage, tailleCases + 1, tailleCases + 1, null);
                gui.addGraphicalElement(image);
                break;
            case HABITAT:
                pathImage = "images/cherrygrove.png";
                image = new ImageElement(x, y, pathImage, tailleCases + 1, tailleCases + 1, null);
                gui.addGraphicalElement(image);
                pathImage = "images/barktown.png";
                image = new ImageElement(x, y, pathImage, tailleCases + 1, tailleCases + 1, null);
                gui.addGraphicalElement(image);
                break;
            default:
                System.out.println("Nature du terrain non reconnue. Fermeture de la fenêtre graphique");
                //lever exception
                break;
        }
    }

    private void drawCarte() {
        Carte carte = data.getCarte();

        for (int i = 0; i < carte.getNbLignes(); i++) {
            for (int j = 0; j < carte.getNbColonnes(); j++) {
                drawCase(j * tailleCases, i * tailleCases, carte.getCase(i, j).getNatureTerrain());
            }
        }
    }

    private void drawRobot(robots.Robot R) {
        String pathImage = R.getFileOfRobot() + "SUD1.png";
        int x = R.getPosition().getLigne() * tailleCases;
        int y = R.getPosition().getColonne() * tailleCases;
        ImageElement image = new ImageElement(y, x, pathImage, tailleCases + 1, tailleCases + 1, null);
        coordImageRobot[data.getListeRobots().indexOf(R)][0] = y;
        coordImageRobot[data.getListeRobots().indexOf(R)][1] = x;
        gui.addGraphicalElement(image);
    }

    private void drawListRobots() {
        for (robots.Robot it : data.getListeRobots()) {
            drawRobot(it);
        }
    }

    private void drawIncendie(Incendie inc) {
        String pathImage = "images/fire.png";
        int x = inc.getCaseIncendie().getLigne() * tailleCases;
        int y = inc.getCaseIncendie().getColonne() * tailleCases;
        ImageElement image = new ImageElement(y, x, pathImage, tailleCases + 1, tailleCases + 1, null);
        gui.addGraphicalElement(image);
    }

    private void drawListIncendies() {
        for (Incendie it : data.getListeIncendies()) {
            drawIncendie(it);
        }
    }
    
    private void refreshIncendies(){
    	int x, y;
    	for(Incendie inc : data.getListeIncendies()) {
    		x = inc.getCaseIncendie().getLigne() * tailleCases;
    		y = inc.getCaseIncendie().getColonne() * tailleCases;
    		if(inc.getNbLitres() > 0) {
    			ImageElement image = new ImageElement(y, x, "images/fire.png", tailleCases+1, tailleCases+1, null);
        		gui.addGraphicalElement(image);
    		} else {
    			//On pourrait afficher de la fumée
    		}
    	}
    }
    private void refreshRobots() throws ErreurPosition{
    //Ce refresh à lieu toutes les GestionnaireEvents.h minutes
    	Robot rob;
    	for(int i = 0; i < data.getListeRobots().size(); i++){
    		rob = data.getListeRobots().get(i);
    		switch(rob.getAction()){
    		case DEPLACE:
    			bougeRobot(rob, i);
    			break;
    		case REMPLIR:
    			remplitReservoir(rob, i, (int) (GE.getPasDeTemps() * rob.getCapaciteMax()/rob.getTempsRemplissageComp()) +1);
    			break;
    		case VERSER:
    			verseReservoir(rob, i, (int) (GE.getPasDeTemps() * rob.getInterventionUnitaire() * 60)+1);
    			break;
    		case INNOCUPE:
    			//On réactualise juste l'image du robot, et on le met de face pour montrer qu'il attend de nouvelles instructions
    			String pathImage = rob.getFileOfRobot() + "SUD1.png";
    			ImageElement image = new ImageElement(coordImageRobot[i][0], coordImageRobot[i][1], pathImage, tailleCases + 1, tailleCases + 1, null);
    			gui.addGraphicalElement(image);
    			break;
    		default:
    			System.out.println("Action non reconnue.");
    			break;
    		}
    	}
    }
    
    private void bougeRobot(Robot rob, int indexRob) throws ErreurPosition{
    	double vitesse = rob.getVitesse(rob.getPosition().getNatureTerrain());
    	int distanceParcourue = (int) (vitesse*GE.getPasDeTemps()*1000/60); //distance parcourue à echelle réelle
    	distanceParcourue = distanceParcourue*tailleCases/data.getCarte().getTailleCases();//distance parcourue à échelle de la carte
    	int depX = coordImageRobot[indexRob][0];
    	int depY = coordImageRobot[indexRob][1];
    	int arriveX = (rob.getPosition().getColonne()+rob.getDirection().getY())*tailleCases;
    	int arriveY = (rob.getPosition().getLigne()+rob.getDirection().getX())*tailleCases;
    	
    	if(rob.getDirection().getX() == 0){
    		//DEPLACEMENT HORIZONTAL
    		int distanceRestante = Math.abs(depX-arriveX); 
    		if(distanceParcourue >= distanceRestante){
    			//on est arrivés
    			coordImageRobot[indexRob][0] += rob.getDirection().getY()*distanceRestante;
    			int lig = rob.getPosition().getLigne();
    			int col = rob.getPosition().getColonne()+rob.getDirection().getY();
    			rob.setPosition(data.getCarte().getCase(lig, col));
    			rob.switchAction(Action.INNOCUPE);
    		}
    		else {
    			//on est pas arrivés
    			coordImageRobot[indexRob][0] += rob.getDirection().getY()*distanceParcourue; 
    		}
    	}
    	else {
    		//DEPLACEMENT VERTICAL
    		int distanceRestante = Math.abs(depY-arriveY);
    		if(distanceParcourue >= distanceRestante){
    			//on est arrivés
    			coordImageRobot[indexRob][1] += rob.getDirection().getX()*distanceRestante;
    			int lig = rob.getPosition().getLigne()+rob.getDirection().getX();
    			int col = rob.getPosition().getColonne();
    			rob.setPosition(data.getCarte().getCase(lig, col));
    			rob.switchAction(Action.INNOCUPE);
    		}
    		else {
    			//on est pas arrivés
    			coordImageRobot[indexRob][1] += rob.getDirection().getX()*distanceParcourue; 
    		}
    	}
    	String pathImage = rob.getFileOfRobot() + rob.getDirection().toString() + indiceImage + ".png";
    	indiceImage = (indiceImage+1)%2;
    	ImageElement image = new ImageElement(coordImageRobot[indexRob][0], coordImageRobot[indexRob][1], pathImage, tailleCases + 1, tailleCases + 1, null);
    	
    	gui.addGraphicalElement(image);
    }
    
    private void remplitReservoir(Robot rob, int indexRob, int qte){
    	String pathImage;
    	if(rob.getDirection() == null) {
    		pathImage = rob.getFileOfRobot() + "SUD" + indiceImage + ".png";
    	} else {
    		pathImage = rob.getFileOfRobot() + rob.getDirection().toString() + indiceImage + ".png";
    	}
    	//Elements graphiques
    	ImageElement image = new ImageElement(coordImageRobot[indexRob][0], coordImageRobot[indexRob][1], pathImage, tailleCases + 1, tailleCases + 1, null);
    	gui.addGraphicalElement(image);
    	pathImage = "images/remplir.png";
    	image = new ImageElement(coordImageRobot[indexRob][0], coordImageRobot[indexRob][1], pathImage, tailleCases + 1, tailleCases + 1, null);
    	gui.addGraphicalElement(image);
    	//On remplit le robot
    	rob.remplirReservoir(qte);
    	
    	//On ajoute une "alarme graphique" si le robot est totalement remplit.
    	if(rob.getCapaciteMax() == rob.getVolumeRestant()){
    		pathImage = "images/bubble.png";
    		image = new ImageElement(coordImageRobot[indexRob][0], coordImageRobot[indexRob][1]+tailleCases, pathImage, tailleCases + 1, tailleCases + 1, null);
    		gui.addGraphicalElement(image);
    		//Ajouter un wait pour qu'on ai le temps de voir l'alarme
    		rob.switchAction(Action.INNOCUPE);
    	}
    }
    
    private void verseReservoir(Robot rob, int indexRob, int qte){
    	String pathImage;
    	if(rob.getDirection() == null) {
    		pathImage = rob.getFileOfRobot() + "SUD" + indiceImage + ".png";
    	} else {
    		pathImage = rob.getFileOfRobot() + rob.getDirection().toString() + indiceImage + ".png";
    	}
    	//Elements graphiques
    	ImageElement image = new ImageElement(coordImageRobot[indexRob][0], coordImageRobot[indexRob][1], pathImage, tailleCases + 1, tailleCases + 1, null);
    	gui.addGraphicalElement(image);
    	pathImage = "images/verser.png";
    	image = new ImageElement(coordImageRobot[indexRob][0], coordImageRobot[indexRob][1], pathImage, tailleCases + 1, tailleCases + 1, null);
    	gui.addGraphicalElement(image);
    	//On remplit le robot
    	if(rob.getVolumeRestant() < qte){
    		rob.deverserEau(rob.getVolumeRestant());
    	} else {
    		rob.deverserEau(qte);
    	}
    	
    	//Si le reservoir du robot est vide ou que l'incendie est éteint, alors il devient innocupé
    	if(rob.getVolumeRestant() <= 0 || data.getIncendie(rob.getPosition()).getNbLitres() <= 0){
    		rob.switchAction(Action.INNOCUPE);
    	}
    }
}
