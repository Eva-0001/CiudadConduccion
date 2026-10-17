package com.graphics; // Agrupa las cuatro lecciones dentro del mismo paquete.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar teclas y cambiar el título de la ventana.

/**
 * CLASE 2: CREACIÓN Y MOVIMIENTO DEL AUTO.
 * Hereda la ciudad de clase1 y agrega únicamente lo necesario para conducir.
 * Orden de lectura: variables, teclado, movimiento, colisiones, cámara y
 * dibujo.
 */
public class CiudadConAuto extends CiudadBase {

    // ==================== 1. VARIABLES DEL AUTO ====================
    protected float autoX = -30; // Posición horizontal inicial: centro de una calle.
    protected float autoZ = 30; // Posición inicial sobre el eje que recorre el fondo de la ciudad.
    protected float angulo = 0; // Orientación en radianes; cero apunta hacia -Z.
    protected float velocidad = 0; // Unidades por segundo; un valor negativo significa reversa.
    protected boolean camaraAerea = false; // false: seguir el auto; true: observar toda la ciudad.
    protected static final float RADIO_AUTO = 1.65f; // Radio que contiene al vehículo para las colisiones.

    protected float frenteX() {
        return -(float) Math.sin(angulo);
    }

    protected float frenteZ() {
        return -(float) Math.cos(angulo);
    }

    // ==================== 2. TECLADO Y REINICIO ====================

    /** Atiende acciones que deben ocurrir una sola vez por pulsación. */
    @Override // Sustituye el método de clase1 y conserva sus acciones mediante super.
    protected void tecla(int key) {
        super.tecla(key); // Ejecuta el control ESC definido en clase1.

        if (key == GLFW_KEY_C) { // Comprueba si se presionó la tecla de cámara.
            camaraAerea = !camaraAerea; // Invierte el modo de cámara actual.
        }

        if (key == GLFW_KEY_R) { // Comprueba si el usuario quiere comenzar de nuevo.
            reiniciar(); // Restaura las variables de conducción.
        }
    }

    /** Coloca nuevamente el auto en su punto de partida. */
    protected void reiniciar() {
        autoX = -30; // Recupera la coordenada X de inicio.
        autoZ = 30; // Recupera la coordenada Z de inicio.
        angulo = 0; // Orienta el frente hacia -Z.
        velocidad = 0; // Detiene cualquier movimiento previo.
    }

    // ==================== 3. MOVIMIENTO POR CUADRO ====================

    /**
     * Actualiza la conducción; deltaTime contiene los segundos transcurridos entre
     * cuadros.
     */
    @Override // Cambia la cámara orbital de clase1 por el control del vehículo.
    protected void actualizar(float deltaTime) {
        acelerar(deltaTime); // Aplica aceleración y resistencia a la velocidad.
        girar(deltaTime); // Modifica la orientación según el volante.
        mover(deltaTime); // Avanza hacia la nueva posición si no hay colisión.
        actualizarTitulo(); // Muestra los controles y la velocidad actual.
    }

    /**
     * Calcula la aceleración del motor y aplica resistencia o frenado a la
     * velocidad.
     */
    private void acelerar(float deltaTime) {
        float acelerador = 0; // Sin teclas pulsadas no se aplica aceleración del motor.

        if (pulsada(GLFW_KEY_W) || pulsada(GLFW_KEY_UP)) { // Acepta W o flecha arriba para avanzar.
            acelerador += 1; // Solicita aceleración hacia delante.
        }

        if (pulsada(GLFW_KEY_S) || pulsada(GLFW_KEY_DOWN)) { // Acepta S o flecha abajo para retroceder.
            acelerador -= 1; // Primero reduce la velocidad positiva y después entra en reversa.
        }

        velocidad += acelerador * 9 * deltaTime; // Integra la aceleración de 9 unidades por segundo cuadrado.
        float resistencia = 0.7f; // Define la pérdida de velocidad normal al rodar.

        if (pulsada(GLFW_KEY_SPACE)) { // Detecta si el usuario mantiene presionado el freno.
            resistencia = 7; // Aumenta la pérdida de velocidad para detenerse rápidamente.
        }

        float factorFrenado = (float) Math.exp(-resistencia * deltaTime); // Calcula la fracción de velocidad
                                                                          // conservada.
        velocidad *= factorFrenado; // Aplica resistencia de forma proporcional al tiempo transcurrido.
        velocidad = Math.max(-6, Math.min(16, velocidad)); // Limita la reversa a -6 y el avance a 16.
    }

    /** Actualiza la orientación del vehículo según el giro y la velocidad. */
    private void girar(float deltaTime) {
        float direccion = 0; // Sin dirección presionada el volante permanece recto.

        if (pulsada(GLFW_KEY_A) || pulsada(GLFW_KEY_LEFT)) { // Comprueba el giro hacia la izquierda.
            direccion += 1; // Selecciona el sentido positivo de rotación.
        }

        if (pulsada(GLFW_KEY_D) || pulsada(GLFW_KEY_RIGHT)) { // Comprueba el giro hacia la derecha.
            direccion -= 1; // Selecciona el sentido negativo de rotación.
        }

        float siguienteAngulo = angulo + direccion * velocidad * 0.11f * deltaTime; // Gira según la velocidad; en
                                                                                    // reversa invierte el giro.
        if (vehiculoPuedeCircular(autoX, autoZ, siguienteAngulo)) {
            angulo = siguienteAngulo; // Acepta el giro si la chata no colisiona con la nueva orientación.
        }
    }

    /**
     * Desplaza el vehículo en la dirección actual y comprueba colisiones de auto y
     * chata.
     */
    private void mover(float deltaTime) {
        float siguienteX = autoX + frenteX() * velocidad * deltaTime; // Propone la nueva posición X.
        float siguienteZ = autoZ + frenteZ() * velocidad * deltaTime; // Propone la nueva posición Z.

        if (vehiculoPuedeCircular(siguienteX, siguienteZ, angulo)) { // Comprueba auto y chata antes de mover.
            autoX = siguienteX; // Acepta el desplazamiento horizontal.
            autoZ = siguienteZ; // Acepta el desplazamiento en profundidad.
        } else { // La posición propuesta invadiría una manzana o saldría del mapa (el auto o la
                 // chata).
            velocidad = 0; // Detiene el auto conservando su última posición válida.
        }
    }

    /**
     * Compone el texto de la ventana sin mezclarlo con las fórmulas de conducción.
     */
    private void actualizarTitulo() {
        int kilometrosPorHora = Math.round(Math.abs(velocidad) * 3.6f); // Convierte m/s a km/h y redondea.
        String titulo = getClass().getSimpleName(); // Obtiene el nombre de la etapa que se está ejecutando.
        titulo += " | " + kilometrosPorHora + " km/h"; // Añade la magnitud de la velocidad al inicio.
        titulo += estadoExtra(); // Permite a clase3 y clase4 añadir luces y entregas.
        titulo += " | WASD/flechas: conducir | Espacio: freno | C: camara | R: reiniciar"; // Añade los controles al
                                                                                           // final.
        glfwSetWindowTitle(ventana, titulo); // Publica el texto en la barra superior de la ventana.
    }

    /** Deja un espacio para los indicadores de las próximas lecciones. */
    protected String estadoExtra() {
        return ""; // En clase2 todavía no hay información adicional.
    }

    // ==================== 4. COLISIONES CON LA CIUDAD ====================

    /**
     * Comprueba si un círculo con centro (x, z) y radio dado cabe sin tocar
     * manzanas ni bordes.
     */
    protected boolean puedeCircular(float x, float z, float radio) {
        float limitePermitido = LIMITE - radio; // Reserva espacio para que el objeto quede dentro.

        if (Math.abs(x) > limitePermitido || Math.abs(z) > limitePermitido) { // Detecta salida por cualquier borde.
            return false; // Rechaza la posición exterior.
        }

        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre las filas del mapa.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre las celdas de cada fila.
                if (MAPA[fila][columna] == CALLE) { // Una celda con cero representa calle.
                    continue; // Omite la calle porque no es un obstáculo.
                }

                float centroX = centro(columna); // Convierte la columna al centro X de la manzana.
                float centroZ = centro(fila); // Convierte la fila al centro Z de la manzana.
                float cercaX = Math.max(centroX - 5, Math.min(x, centroX + 5)); // Busca el X más cercano dentro de la
                                                                                // acera.
                float cercaZ = Math.max(centroZ - 5, Math.min(z, centroZ + 5)); // Busca el Z más cercano dentro de la
                                                                                // acera.
                float distanciaX = x - cercaX; // Calcula la separación horizontal del objeto al rectángulo.
                float distanciaZ = z - cercaZ; // Calcula la separación en profundidad al rectángulo.
                float distanciaCuadrada = distanciaX * distanciaX + distanciaZ * distanciaZ; // Aplica Pitágoras sin
                                                                                             // raíz.

                if (distanciaCuadrada < radio * radio) { // Comprueba si el círculo invade la manzana.
                    return false; // Rechaza el movimiento que produciría una colisión.
                }
            }
        }

        return true; // Acepta la posición porque no se encontró ningún obstáculo.
    }

    /**
     * Comprueba si el círculo del auto cabe en una posición sin tocar manzanas ni
     * bordes.
     */
    protected boolean puedeCircular(float x, float z) {
        return puedeCircular(x, z, RADIO_AUTO);
    }

    /**
     * <------------------------------------------------------------------------->
     * Comprueba si la chata o su lanza colisionan con manzanas o bordes para una
     * posición y orientación dadas.
     */
    protected boolean chataColisiona(float x, float z, float ang) {
        float seno = (float) Math.sin(ang);
        float coseno = (float) Math.cos(ang);

        // Muestreo a lo largo del remolque: lanza, frente, centro y portón trasero
        // <----------------------------------------------------------->
        float[][] puntosChata = {
                { 1.85f, 0.50f }, // Lanza / conexión
                { 2.40f, 0.85f }, // Frente de la caja
                { 2.70f, 0.88f }, // Centro y ruedas de la chata
                { 3.20f, 0.85f }, // Parte posterior de la caja
                { 3.50f, 0.75f } // Portón trasero y luces
        };

        for (float[] punto : puntosChata) {
            float offsetZ = punto[0];
            float radio = punto[1];
            float px = x + seno * offsetZ;
            float pz = z + coseno * offsetZ;
            if (!puedeCircular(px, pz, radio)) {
                return true; // Hay colisión en la chata
            }
        }
        return false; // La chata está libre de colisiones
    }

    //
    /**
     * Comprueba que tanto el auto como la chata puedan circular sin colisiones.
     */
    protected boolean vehiculoPuedeCircular(float x, float z, float ang) {
        return puedeCircular(x, z) && !chataColisiona(x, z, ang);
    }

    // ==================== 5. CÁMARA ====================

    /** Elige entre una vista general y una cámara situada detrás del auto. */
    @Override // Personaliza la cámara creada en clase1.
    protected void configurarCamara() {
        if (camaraAerea) { // Comprueba si está activa la vista general.
            super.configurarCamara(); // Reutiliza la cámara oblicua de la primera lección.
            return; // Evita reemplazarla con la cámara de seguimiento.
        }

        float camaraX = autoX + (float) Math.sin(angulo) * 12; // Coloca la cámara 12 unidades detrás en X.
        float camaraZ = autoZ + (float) Math.cos(angulo) * 12; // Coloca la cámara 12 unidades detrás en Z.
        vector("uOjo", camaraX, 9, camaraZ); // Envía la posición de la cámara, a 9 unidades de altura.
        vector("uObjetivo", autoX, 0.8f, autoZ); // Orienta la cámara hacia la carrocería.
        decimal("uAspecto", (float) ancho / alto); // Mantiene las proporciones al redimensionar la ventana.
    }

    // ==================== 6. DIBUJO DEL AUTO ====================

    /** Dibuja primero la ciudad existente y luego el vehículo. */
    @Override // Amplía el dibujo de clase1 sin copiar su código.
    protected void escena() {
        super.escena(); // Dibuja asfalto, calles, edificios y parques.
        dibujarAuto(); // Añade el modelo del vehículo sobre la ciudad.
    }

    /**
     * Construye el auto con cajas; argumentos: posición XYZ, tamaño XYZ y color
     * RGB.
     */
    protected void dibujarAuto() {
        pieza(0, 0.65f, 0, 1.65f, 0.55f, 2.6f, 0.95f, 0.24f, 0.12f); // Dibuja la carrocería roja.
        pieza(0, 1.12f, 0.12f, 1.3f, 0.55f, 1.25f, 0.22f, 0.65f, 0.78f); // Dibuja la cabina azulada.
        dibujarChataRemolque(); // Agrega el remolque / acoplado de carga ("chata") enganchado atras.
        float[] ladosRuedas = { -0.88f, 0.88f }; // Ubica ruedas a izquierda y derecha del auto.
        float[] ejesRuedas = { -0.82f, 0.82f }; // Ubica las ruedas delanteras y traseras.

        for (float x : ladosRuedas) { // Selecciona uno de los dos lados del vehículo.
            for (float z : ejesRuedas) { // Selecciona el eje delantero o trasero.
                pieza(x, 0.38f, z, 0.24f, 0.58f, 0.6f, 0.055f, 0.065f, 0.08f); // Dibuja una rueda oscura.
            }
        }

        float[] ladosFaros = { -0.55f, 0.55f }; // Define la separación lateral de las luces.
        for (float x : ladosFaros) { // Repite el dibujo para ambos lados.
            pieza(x, 0.68f, -1.32f, 0.38f, 0.2f, 0.07f, 1, 0.95f, 0.65f); // Dibuja un faro delantero claro.
            pieza(x, 0.68f, 1.32f, 0.35f, 0.17f, 0.07f, 0.85f, 0.05f, 0.05f); // Dibuja una luz trasera roja.
        }
    }

    /**
     * Construye el remolque / acoplado ("chata" de tiro) enganchado en la parte
     * trasera del auto.
     */
    protected void dibujarChataRemolque() {
        // --- 1. Enganche y lanza de tiro (conecta el auto con la chata) ---
        pieza(0, 0.45f, 1.38f, 0.12f, 0.12f, 0.14f, 0.25f, 0.25f, 0.28f); // Bocha de enganche en el paragolpes.
        pieza(0, 0.43f, 1.62f, 0.10f, 0.08f, 0.44f, 0.45f, 0.45f, 0.48f); // Barra central de la lanza.
        pieza(-0.25f, 0.43f, 1.72f, 0.08f, 0.08f, 0.36f, 0.40f, 0.40f, 0.42f); // Brazo izquierdo de la lanza en V.
        pieza(0.25f, 0.43f, 1.72f, 0.08f, 0.08f, 0.36f, 0.40f, 0.40f, 0.42f); // Brazo derecho de la lanza en V.

        // --- 2. Plataforma y estructura de la chata (Z central = 2.70) ---
        pieza(0, 0.38f, 2.70f, 1.45f, 0.10f, 0.10f, 0.20f, 0.20f, 0.22f); // Eje de ruedas del remolque.
        pieza(0, 0.48f, 2.70f, 1.45f, 0.10f, 1.60f, 0.30f, 0.30f, 0.32f); // Plataforma / piso de la chata.

        // --- 3. Barandas de madera y esquineros de refuerzo ---
        float colorBarandaR = 0.80f, colorBarandaG = 0.50f, colorBarandaB = 0.22f; // Tono madera cálida.
        pieza(0, 0.72f, 1.92f, 1.45f, 0.38f, 0.08f, colorBarandaR, colorBarandaG, colorBarandaB); // Baranda delantera.
        pieza(0, 0.72f, 3.48f, 1.45f, 0.38f, 0.08f, colorBarandaR, colorBarandaG, colorBarandaB); // Portón trasero.
        pieza(-0.70f, 0.72f, 2.70f, 0.08f, 0.38f, 1.56f, colorBarandaR, colorBarandaG, colorBarandaB); // Lateral
                                                                                                       // izquierdo.
        pieza(0.70f, 0.72f, 2.70f, 0.08f, 0.38f, 1.56f, colorBarandaR, colorBarandaG, colorBarandaB); // Lateral
                                                                                                      // derecho.

        // Postes esquineros metálicos oscuros
        float posteY = 0.72f, posteL = 0.40f;
        pieza(-0.70f, posteY, 1.92f, 0.10f, posteL, 0.10f, 0.20f, 0.20f, 0.22f); // Esquina delantera izquierda.
        pieza(0.70f, posteY, 1.92f, 0.10f, posteL, 0.10f, 0.20f, 0.20f, 0.22f); // Esquina delantera derecha.
        pieza(-0.70f, posteY, 3.48f, 0.10f, posteL, 0.10f, 0.20f, 0.20f, 0.22f); // Esquina trasera izquierda.
        pieza(0.70f, posteY, 3.48f, 0.10f, posteL, 0.10f, 0.20f, 0.20f, 0.22f); // Esquina trasera derecha.

        // --- 4. Ruedas y guardabarros de la chata ---
        float[] ladosChata = { -0.84f, 0.84f };
        for (float x : ladosChata) {
            pieza(x, 0.38f, 2.70f, 0.20f, 0.58f, 0.58f, 0.055f, 0.065f, 0.08f); // Rueda oscura.
            float offsetLlanta = x < 0 ? -0.06f : 0.06f;
            pieza(x + offsetLlanta, 0.38f, 2.70f, 0.04f, 0.28f, 0.28f, 0.75f, 0.75f, 0.78f); // Llanta plateada.
            pieza(x * 0.95f, 0.70f, 2.70f, 0.14f, 0.07f, 0.68f, 0.25f, 0.25f, 0.28f); // Guardabarros.
        }

        // --- 5. Luces traseras y matrícula de la chata ---
        pieza(-0.50f, 0.60f, 3.53f, 0.20f, 0.14f, 0.05f, 0.90f, 0.10f, 0.08f); // Luz trasera izquierda.
        pieza(0.50f, 0.60f, 3.53f, 0.20f, 0.14f, 0.05f, 0.90f, 0.10f, 0.08f); // Luz trasera derecha.
        pieza(0, 0.56f, 3.53f, 0.28f, 0.10f, 0.04f, 0.95f, 0.85f, 0.30f); // Placa de matrícula / reflector.

        // --- 6. Carga en la chata (cajas de encomienda) ---
        pieza(-0.22f, 0.74f, 2.50f, 0.65f, 0.42f, 0.60f, 0.70f, 0.48f, 0.26f); // Caja de carga grande.
        pieza(0.28f, 0.70f, 2.90f, 0.55f, 0.34f, 0.55f, 0.58f, 0.40f, 0.22f); // Caja de carga mediana.
    }

    /** Transforma una pieza del espacio local del auto al espacio de la ciudad. */
    protected void pieza(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(angulo); // Calcula el coseno de la orientación del auto.
        float seno = (float) Math.sin(angulo); // Calcula el seno de la misma orientación.
        float mundoX = autoX + coseno * x + seno * z; // Gira la posición local y suma la posición X del auto.
        float mundoZ = autoZ - seno * x + coseno * z; // Gira la posición local y suma la posición Z del auto.
        cajaGirada(mundoX, y, mundoZ, sx, sy, sz, r, g, b, angulo); // Dibuja la pieza con la orientación del vehículo.
    }

    /** Punto de entrada para ejecutar únicamente la segunda etapa. */
    public static void main(String[] args) {
        CiudadConAuto aplicacion = new CiudadConAuto(); // Crea la aplicación con ciudad y vehículo.
        aplicacion.run(); // Inicia la ventana y el ciclo de dibujo heredados.
    }
}
