package com.graphics; // Permite probar los métodos protegidos del juego desde el mismo paquete.

import java.util.LinkedList; // Proporciona la cola usada por la búsqueda en anchura de celdas.
import java.util.Queue; // Define el tipo de la cola de celdas pendientes de explorar.
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba las reglas del mapa 11x11, colisiones, farolas, destinos y reinicio. */
public class ConduccionTest extends TestCase {

    // ==================== 1. MAPA Y CONSTANTES ====================

    /** Comprueba dimensiones del mapa, conteo de manzanas y la relación LIMITE = filas * CELDA / 2. */
    public void testMapaYConstantes() {
        assertTrue("El mapa debe tener al menos 11 filas", clase1.MAPA.length >= 11); // Verifica el alto mínimo.
        assertTrue("El mapa debe tener al menos 11 columnas", clase1.MAPA[0].length >= 11); // Verifica el ancho mínimo.
        int edificios = 0; // Acumulará las celdas de tipo 1.
        int parques = 0; // Acumulará las celdas de tipo 2.
        for (int[] fila : clase1.MAPA) { // Recorre cada fila del mapa.
            for (int celda : fila) { // Recorre cada celda de esa fila.
                if (celda == 1) edificios++; // Cuenta los edificios encontrados.
                if (celda == 2) parques++; // Cuenta los parques encontrados.
            }
        }
        assertTrue("Debe haber al menos 12 edificios", edificios >= 12); // El mapa 11x11 tiene 19.
        assertTrue("Debe haber al menos 4 parques", parques >= 4); // El mapa 11x11 tiene 6.
        assertEquals("CELDA debe ser 10", 10f, clase1.CELDA, 0f); // La celda no debe haber cambiado.
        float limiteEsperado = clase1.MAPA.length * clase1.CELDA / 2; // Calcula el borde según la fórmula.
        assertEquals("LIMITE debe ser MAPA.length * CELDA / 2", limiteEsperado, clase1.LIMITE, 0.001f); // Verifica la coherencia.
    }

    // ==================== 2. CALLES PRINCIPALES ====================

    /** Comprueba que la calle x=-30 y la perimetral z=-50 son transitables en todo su recorrido. */
    public void testCallesTransitables() {
        clase2 juego = new clase2(); // Crea el estado sin abrir ventana.
        for (float z = -50; z <= 50; z += 0.25f) { // Recorre la calle vertical de oeste medio.
            assertTrue("Calle x=-30 bloqueada en z=" + z, juego.puedeCircular(-30, z)); // Todo el tramo debe ser libre.
        }
        for (float x = -50; x <= 50; x += 0.25f) { // Recorre la avenida norte del mapa.
            assertTrue("Calle z=-50 bloqueada en x=" + x, juego.puedeCircular(x, -50)); // Verifica la conexión este-oeste.
        }
        assertTrue("El punto de partida (-30,30) debe ser transitable", juego.puedeCircular(-30, 30)); // Posición inicial válida.
    }

    // ==================== 3. OBSTÁCULOS Y LÍMITES ====================

    /** Comprueba posiciones bloqueadas por manzanas o bordes, y posiciones libres junto a ellas. */
    public void testObstaculosYLimites() {
        clase2 juego = new clase2(); // Crea un vehículo con su radio de colisión.
        assertFalse("Centro edificio (-40,-40)", juego.puedeCircular(-40, -40)); // MAPA[1][1]=1.
        assertFalse("Centro parque (-40,0)", juego.puedeCircular(-40, 0)); // MAPA[5][1]=2.
        assertFalse("Edificio en (20,0)", juego.puedeCircular(20, 0)); // MAPA[5][7]=1.
        assertFalse("Edificio en (0,0)", juego.puedeCircular(0, 0)); // MAPA[5][5]=1.
        assertFalse("Invade acera col1 (-45.5,-40)", juego.puedeCircular(-45.5f, -40)); // dist2=0.25 < RADIO2.
        assertFalse("Fuera del limite derecho (54,0)", juego.puedeCircular(54, 0)); // 54 > LIMITE-RADIO.
        assertFalse("Fuera del limite norte (0,-56)", juego.puedeCircular(0, -56)); // 56 > LIMITE-RADIO.
        assertTrue("Separacion suficiente (-47,-40)", juego.puedeCircular(-47, -40)); // dist2=4 > RADIO2=2.72.
        assertTrue("Borde interior derecho (53,0)", juego.puedeCircular(53, 0)); // 53 < LIMITE-RADIO y lejos de manzanas.
    }

    // ==================== 4. DESTINOS ALCANZABLES ====================

    /** Comprueba que cada destino es transitable y conectado al inicio por calles usando BFS. */
    public void testDestinosAlcanzables() {
        clase2 juego = new clase2(); // Necesario para llamar a puedeCircular.
        int startCol = (int)((-30 + clase1.LIMITE) / clase1.CELDA); // Convierte X=-30 a índice de columna.
        int startRow = (int)((30 + clase1.LIMITE) / clase1.CELDA); // Convierte Z=30 a índice de fila.
        for (float[] destino : clase4.DESTINOS) { // Itera sobre cada uno de los cinco destinos.
            float dx = destino[0]; // Coordenada X del destino.
            float dz = destino[1]; // Coordenada Z del destino.
            assertTrue("Destino (" + dx + "," + dz + ") no es transitable", juego.puedeCircular(dx, dz)); // Debe estar en calle.
            int destCol = (int)((dx + clase1.LIMITE) / clase1.CELDA); // Convierte X al índice de columna en el mapa.
            int destRow = (int)((dz + clase1.LIMITE) / clase1.CELDA); // Convierte Z al índice de fila en el mapa.
            assertTrue("Destino (" + dx + "," + dz + ") no alcanzable por calles",
                    celdaAlcanzable(startRow, startCol, destRow, destCol)); // BFS de celdas de calle.
        }
    }

    /** Recorre el MAPA en anchura desde la celda inicio hasta la celda destino usando solo celdas de calle. */
    private boolean celdaAlcanzable(int startRow, int startCol, int targetRow, int targetCol) {
        int filas = clase1.MAPA.length; // Número de filas del mapa.
        int cols = clase1.MAPA[0].length; // Número de columnas del mapa.
        boolean[][] visitado = new boolean[filas][cols]; // Marca las celdas ya exploradas.
        Queue<int[]> cola = new LinkedList<>(); // Cola de celdas pendientes de explorar.
        cola.add(new int[]{startRow, startCol}); // Comienza desde la celda de inicio.
        visitado[startRow][startCol] = true; // Marca el inicio como visitado.
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // Las cuatro direcciones cardinales.
        while (!cola.isEmpty()) { // Continúa mientras haya celdas pendientes.
            int[] actual = cola.poll(); // Extrae la siguiente celda a explorar.
            if (actual[0] == targetRow && actual[1] == targetCol) return true; // Destino alcanzado.
            for (int[] d : dirs) { // Explora los cuatro vecinos de la celda actual.
                int nr = actual[0] + d[0]; // Fila del vecino.
                int nc = actual[1] + d[1]; // Columna del vecino.
                if (nr >= 0 && nr < filas && nc >= 0 && nc < cols // Comprueba que el vecino está dentro del mapa.
                        && !visitado[nr][nc] && clase1.MAPA[nr][nc] == 0) { // Solo visita celdas de calle no exploradas.
                    visitado[nr][nc] = true; // Marca el vecino como visitado.
                    cola.add(new int[]{nr, nc}); // Añade el vecino a la cola.
                }
            }
        }
        return false; // No se encontró camino hasta el destino.
    }

    // ==================== 5. FAROLAS ====================

    /** Comprueba que LUCES tiene al menos 9 farolas y que todas están dentro de los límites del mapa. */
    public void testLuces() {
        assertTrue("Debe haber al menos 9 farolas", clase3.LUCES.length >= 9); // El mapa 11x11 genera 25.
        for (float[] luz : clase3.LUCES) { // Recorre cada posición de farola generada.
            assertTrue("Farola X fuera del mapa: " + luz[0], Math.abs(luz[0]) <= clase1.LIMITE); // X dentro del borde.
            assertTrue("Farola Z fuera del mapa: " + luz[2], Math.abs(luz[2]) <= clase1.LIMITE); // Z dentro del borde.
        }
    }

    // ==================== 6. REINICIO ====================

    /** Comprueba que reiniciar() devuelve posición, velocidad, ángulo y entregas a su estado inicial. */
    public void testReinicio() {
        clase4 juego = new clase4(); // Crea la versión final sin inicializar OpenGL.
        juego.autoX = 12; // Simula un desplazamiento horizontal del auto.
        juego.autoZ = 4; // Simula un desplazamiento en profundidad.
        juego.velocidad = 10; // Simula que el auto avanza a alta velocidad.
        juego.angulo = 2; // Simula que el vehículo giró.
        juego.entregas = 3; // Simula que el jugador ya completó tres entregas antes de reiniciar.
        juego.reiniciar(); // Ejecuta el mismo reinicio que activa la tecla R.
        assertEquals("autoX tras reiniciar", -30f, juego.autoX, 0f); // Recupera la coordenada X inicial.
        assertEquals("autoZ tras reiniciar", 30f, juego.autoZ, 0f); // Recupera la coordenada Z inicial.
        assertEquals("velocidad tras reiniciar", 0f, juego.velocidad, 0f); // El vehículo queda detenido.
        assertEquals("angulo tras reiniciar", 0f, juego.angulo, 0f); // Recupera la dirección frontal inicial.
        assertEquals("entregas tras reiniciar", 0, juego.entregas); // Comprueba que la variable numérica de entregas vuelve a cero.
        assertTrue("entregas debe ser 0 tras reiniciar", // Comprueba que el contador de entregas se reinicia.
                juego.estadoExtra().contains("Entregas 0/")); // estadoExtra muestra "Entregas 0/5" cuando es cero.
    }
}
