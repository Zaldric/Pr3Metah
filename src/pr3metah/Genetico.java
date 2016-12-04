/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pr3metah;

import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author Juanca
 *
 *
 *
 */
class Genetico {

    private int tamPoblacion = 50;
    private int tabu;
    private ArrayList<int[]> poblacion, descendencia;
    private int costes[], costesAux[];
    private double probGen;
    private int nGeneracion;
    private LocalSearch local;

    /**
     * Realiza el algoritmo AGG-Hux para el problema
     *
     * @param x numero de filas de la matriz (zonas)
     * @param y numero de columnas de la matriz (comisarias)
     * @param matriz datos de las comisarías y las zonas que cubren asi como su coste
     * @param cubreOrdenado vector con el numero de zonas que cubre cada
     * comisaria
     * @param optimo coste optimo del fichero que se está evaluando
     * @param alg algoritmo que se esta evaluando
     */
    void AGGHux(int x, int y, int matriz[][], Pair cubreOrdenado[], String optimo, String alg) {
        long time_start, time_end;
        time_start = System.currentTimeMillis();
        poblacion = new ArrayList<>();
        costes = new int[tamPoblacion];
        boolean modificado[] = new boolean[tamPoblacion];
        Random rand = new Random();
        int muta;
        local = new LocalSearch();
        int h1[], h2[];
        int anteriorMejor = 9999999;
        generarPoblacion(x, y, tamPoblacion, cubreOrdenado, matriz);
        for (int i = 1; i < tamPoblacion; i++) {
            modificado[i] = false;
        }

        //Se generan los descendientes
        int z = 0;
        int esperanza = (int) Math.round(0.7 * (tamPoblacion / 2)), restantes;
        while (z < 20000) {
            muta = (Math.abs(rand.nextInt() % tamPoblacion));
            descendencia = new ArrayList<>();
            costesAux = new int[tamPoblacion];
            ++nGeneracion;
            int cont = 0;
            restantes = esperanza;
            for (int i = 0; i < tamPoblacion / 2; i++) {
                tabu = -1;
                int padre1 = torneoBinario();
                int padre2 = torneoBinario();
                if (restantes > 0) {
                    h1 = new int[y];
                    h2 = new int[y];
                    hux(poblacion.get(padre1), poblacion.get(padre2), h1, h2, y);
                    modificado[(i * 2)] = true;
                    modificado[(i * 2) + 1] = true;
                    descendencia.add(h1);
                    descendencia.add(h2);
                    --restantes;
                } else {
                    modificado[(i * 2)] = false;
                    modificado[(i * 2) + 1] = false;
                    descendencia.add(poblacion.get(padre1).clone());
                    descendencia.add(poblacion.get(padre2).clone());
                    costesAux[cont] = costes[padre1];
                    costesAux[cont + 1] = costes[2];
                }
                cont += 2;
            }
            //MUTO
            mutacion(muta, y);
            modificado[muta] = true;
            for (int i = 0; i < tamPoblacion; i++) {
                if (modificado[i]) {
                    reparaSol(x, y, matriz, descendencia.get(i));
                    local.eliminaRedundancias(x, y, matriz, descendencia.get(i), cubreOrdenado, 0);
                    costesAux[i] = calculaSolucion(y, descendencia.get(i), matriz);
                    ++z;
                    if (z % 1000 == 0) {
                        probGen -= 0.01;
                    }
                }
            }

            //AQUI BUSCO EL MEJOR DE LA POBLACION
            int mejorP = 0;
            for (int i = 1; i < tamPoblacion; i++) {
                if (costes[mejorP] > costes[i]) {
                    mejorP = i;
                }
            }
            //AQUI BUSCO EL PEOR DE LOS DESCENDIENTES
            int peorD = 0;
            for (int i = 1; i < tamPoblacion; i++) {
                if (costesAux[peorD] < costesAux[i]) {
                    peorD = i;
                }
            }

            //GUARDO EL ELITISMO
            if (costes[mejorP] < costesAux[peorD]) {
                costesAux[peorD] = costes[mejorP];
                descendencia.set(peorD, poblacion.get(mejorP).clone());
            }

            //AQUI INTERCAMBIO LAS POBLACIONES
            //poblacion.clear();
            for (int i = 0; i < descendencia.size(); ++i) {
                poblacion.set(i, descendencia.get(i).clone());
            }
            System.arraycopy(costesAux, 0, costes, 0, tamPoblacion);

            //BUSCO EL MEJOR DE LOS DESCENDIENTES A VER SI HE ENCONTRADO UNO MEJOR 
            int mejorD = costesAux[0];
            for (int i = 1; i < tamPoblacion; ++i) {
                if (costesAux[i] < mejorD) {
                    mejorD = costesAux[i];
                }
            }
            //SI SE ENCUENTRA UN RESULTADO MEJOR A LOS ANTERIORES
            if (mejorD < anteriorMejor) {
                nGeneracion = 0;
                anteriorMejor = mejorD;
            }
            if (reinicializarEstanc() || reinicializarConv()) {
                int mejor[] = poblacion.get(mejorP).clone();
                int aux = costes[mejorP];
                poblacion.clear();
                costes = new int[tamPoblacion];
                generarPoblacion(x, y, tamPoblacion - 1, cubreOrdenado, matriz);
                poblacion.add(mejor);
                costes[tamPoblacion - 1] = aux;
            }
        }
        time_end = System.currentTimeMillis();
        //PARA SABER QUIEN ES EL MEJOR
        int mejor = costes[0];
        for (int i = 1; i < tamPoblacion; ++i) {
            if (costes[i] < mejor) {
                mejor = costes[i];
            }
        }
        System.out.println("ALGORITMO: : " + alg);
        System.out.println("FIN DEL ALGORITMO, EL MEJOR COSTE ES DE: " + mejor + " EL OPTIMO ERA: " + optimo + " SE HAN HECHO " + z + " ITERACCIONES");
        System.out.println("HA TARDADO " + (float) (time_end - time_start) / 1000.0 + " SEGUNDOS");
    }


    /**
     * Realiza el cruce hux entre dos individuos generando dos hijos
     *
     * @param padre vector con el primer individuo
     * @param madre vector con el segundo individuo
     * @param hijo1 vector con el hijo1 generado
     * @param hijo2 vector con el hijo2 generado
     * @param tam tamanio de los vectores
     */
    private void hux(int padre[], int madre[], int hijo1[], int hijo2[], int tam) {
        int paridad = 0;
        for (int i = 1; i < tam; ++i) {
            if (padre[i] != madre[i]) {
                if ((paridad % 2) == 0) {
                    hijo1[i] = padre[i];
                    hijo2[i] = madre[i];
                } else {
                    hijo1[i] = madre[i];
                    hijo2[i] = madre[i];
                }
                ++paridad;
            } else {
                hijo1[i] = hijo2[i] = padre[i];
            }
        }
    }

    /**
     * Comprueba si nuestra poblacion esta estancada
     *
     * @return si hay que reinicializar por estancamiento o no
     */
    private boolean reinicializarEstanc() {
        if (nGeneracion >= 20) {
            nGeneracion = 0;
            return true;
        }
        return false;
    }

    /**
     * Comprueba si nuestra poblacion converge en una misma solución
     *
     * @return si hay que reinicializar por convergencia o no
     */
    private boolean reinicializarConv() {
        Pair reinicializacion[];
        int tamReinicio = 0, donde = 0;
        reinicializacion = new Pair[tamPoblacion];
        boolean encontrado;

        for (int i = 0; i < tamPoblacion; i++) {
            encontrado = false;
            for (int j = 0; j < tamReinicio; j++) {
                if (reinicializacion[j].getLugar() == costesAux[i]) {
                    encontrado = true;
                    donde = j;
                }
            }
            if (encontrado) {
                reinicializacion[donde].aumentaCubre();
            } else {
                reinicializacion[tamReinicio] = new Pair(costesAux[i], 1);
                ++tamReinicio;
            }
        }

        float porc = (float) (tamPoblacion * 0.8);
        for (int i = 0; i < tamReinicio; i++) {
            if (reinicializacion[i].getCubre() >= porc) {
                return true;
            }
        }
        return false;
    }


    /**
     * Repara un cromosoma para que sea solucion
     *
     * @param x numero de filas de la matriz (zonas)
     * @param y numero de columnas de la matriz (comisarias)
     * @param matriz datos de las comisarías y las zonas que cubren asi como su coste
     * @param sol vector solucion que se esta reparando
     */
    private void reparaSol(int x, int y, int matriz[][], int sol[]) {
        while (true) {
            int cubiertos[] = new int[x];
            for (int i = 1; i < x; i++) {
                cubiertos[i] = 0;
            }
            for (int c = 1; c < y; c++) {
                if (sol[c] == 1) {
                    for (int f = 1; f < x; f++) {
                        if (matriz[f][c] == 1) {
                            cubiertos[f] = 1;
                        }
                    }
                }
            }
            int cubre[] = new int[y];
            for (int i = 0; i < y; i++) {
                cubre[i] = 0;
            }
            for (int f = 1; f < x; f++) {
                if (cubiertos[f] == 0) {
                    for (int c = 1; c < y; c++) {
                        if (matriz[f][c] == 1) {
                            ++cubre[c];
                        }
                    }
                }
            }
            float mayor = (float) cubre[1] / matriz[0][1];
            int pos = 1;
            for (int i = 2; i < y; i++) {
                if (((float) cubre[i] / matriz[0][i]) > mayor) {
                    mayor = (float) cubre[i] / matriz[0][i];
                    pos = i;
                }
            }
            if (mayor == 0) {
                return;
            }
            sol[pos] = 1;
        }
    }

    /**
     * Muta un cromosoma generando uno nuevo
     *
     * @param pos el cromosoma de la poblacion que vamos a mutar
     * @param tam tamanio del vector
     */
    private void mutacion(int pos, int tam) {
        Random rand = new Random();
        float prob;
        for (int i = 1; i < tam; ++i) {
            prob = (float) (Math.abs(rand.nextInt() % 101)) / 100;
            if (prob < probGen) {
                descendencia.get(pos)[i] = (descendencia.get(pos)[i] == 1) ? 0 : 1;
            }
        }
    }

    /**
     * Selecciona por torneo binario un padre entre dos candidatos posibles
     *
     * @return el padre seleccionado
     */
    private int torneoBinario() {
        Random rnd = new Random();
        int n1 = Math.abs(rnd.nextInt() % poblacion.size());
        int n2 = Math.abs(rnd.nextInt() % poblacion.size());
        while (n1 == n2 || n1 == tabu || n2 == tabu) {
            n1 = Math.abs(rnd.nextInt() % poblacion.size());
            n2 = Math.abs(rnd.nextInt() % poblacion.size());
        }
        int n = (costes[n1] <= costes[n2]) ? (1) : (2);
        tabu = n;
        return (n == 1) ? (n1) : (n2);
    }

    /**
     * Genera una poblacion de cromosomas aleatorios de tamaño nPoblacion
     *
     * @param x numero de filas de la matriz (zonas)
     * @param y numero de columnas de la matriz (comisarias)
     * @param nPoblacion tamanio de la poblacion
     * @param cubreOrdenado vector con el numero de zonas que cubre cada
     * comisaria
     * @param matriz datos de las comisarías y las zonas que cubren asi como su coste
     */
    private void generarPoblacion(int x, int y, int nPoblacion, Pair cubreOrdenado[], int matriz[][]) {
        for (int i = 0; i < nPoblacion; ++i) {
            poblacion.add(generarCromosoma(x, y, cubreOrdenado, matriz, i));
        }
    }

    /**
     * Genera un cromosoma construido aleatoriamente
     *
     * @param x numero de filas de la matriz (zonas)
     * @param y numero de columnas de la matriz (comisarias)
     * @param cubreOrdenado vector con el numero de zonas que cubre cada
     * comisaria
     * @param matriz datos de las comisarías y las zonas que cubren asi como su coste
     * @param num el numero de cromosoma que estamos generando 
     * @return      
     * 
     */
    private int[] generarCromosoma(int x, int y, Pair cubreOrdenado[], int matriz[][], int num) {
        int cromo[] = new int[y];
        int coste = 0;
        Random rnd = new Random();
        int nR, n;
        for (int i = 1; i < y; i++) {
            cromo[i] = 0;
        }
        ArrayList<Integer> array = new ArrayList<>();
        for (int i = 1; i < y; i++) {
            array.add(i);
        }
        do {
            nR = Math.abs(rnd.nextInt() % array.size());
            n = array.remove(nR);
            ++cromo[n];
            coste += matriz[0][n];
        } while (!esSolucion(x, y, matriz, cromo));
        local.eliminaRedundancias(x, y, matriz, cromo, cubreOrdenado, 0);

        costes[num] = coste;
        return cromo;
    }

    /**
     * Comprueba si un cromosoma es solucion
     *
     * @param x numero de filas de la matriz (zonas)
     * @param y numero de columnas de la matriz (comisarias)
     * @param solucion el cromosoma que estamos evaluando
     * @param matriz datos de las comisarías y las zonas que cubren asi como su coste
     * @return  si el cromosoma dado es solucion o no
     */
    private boolean esSolucion(int x, int y, int matriz[][], int solucion[]) {
        boolean ok;
        for (int i = 1; i < x; i++) {
            ok = false;
            for (int j = 1; j < y; j++) {
                if (solucion[j] == 1) {
                    if (matriz[i][j] == 1) {
                        j = y;
                        ok = true;
                    }
                }
            }
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calcula el coste de un cromosoma (solucion)
     *
     * @param solucion el cromosoma que estamos evaluando
     * @param mat datos de las comisarías y las zonas que cubren asi como su coste
     * @param y numero de columnas de la matriz (comisarias)
     * @return      
     */
    private int calculaSolucion(int y, int solucion[], int mat[][]) {
        int coste = 0;
        for (int i = 1; i < y; i++) {
            if (solucion[i] == 1) {
                coste += mat[0][i];
            }
        }
        return coste;
    }
}