/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package robots;

import carte.*;

/**
 * *****************************
 */
/* CLASSE FILLE : ROBOT A ROUES */
/**
 * *****************************
 */
public class RobotARoues extends Robot {

    @Override
    protected void setVitesseDefaut(double v) {
        if (v >= 0) {
            this.vitesseDefaut = v;
        } else {
            this.vitesseDefaut = 80;
        }
    }

    @Override
    public double getVitesse(NatureTerrain nature) {
        if (nature == NatureTerrain.TERRAIN_LIBRE || nature == NatureTerrain.HABITAT) {
            return this.vitesseDefaut;
        } else {
            return 0;
        }
    }

    /*Constructeurs */
    public RobotARoues(Case pos) {
        super(pos);
        this.capaciteMax = 5000;
        this.tempsRemplissageComp = 10;
        this.interventionUnitaire = 20;
        this.volumeRestant = this.capaciteMax;
    }

    public RobotARoues(Case pos, double vitesse) {
        this(pos);
        setVitesseDefaut(vitesse);
    }

    @Override
    public void remplirReservoir(int qte) {
        if (peutRemplirReservoir()) {
            if (this.volumeRestant + qte <= this.capaciteMax) {
                this.volumeRestant += qte;
            } else {
                this.volumeRestant = this.capaciteMax;
            }
        }
    }

    @Override
    public boolean peutRemplirReservoir() {
        Carte carte = donnees.getCarte();
        for (Direction direc : Direction.values()) {
            if (carte.voisinExiste(this.position, direc) && carte.getVoisin(this.position, direc).getNatureTerrain() == NatureTerrain.EAU) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getFileOfRobot() {
        return "images/robots/roues/";
    }

}
