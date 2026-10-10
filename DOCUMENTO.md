# Ciudad y Conducción — Documento de entrega

## 1. Cómo ejecutar

**Requisitos:** JDK 17 o superior (probado con JDK 21) y Apache Maven 3.x.

```bash
# Compilar y ejecutar la versión final (clase4, configurada en pom.xml como mainClass)
mvn compile exec:exec

# Ejecutar los tests
mvn test
```

En macOS Maven activa automáticamente el perfil `macos` (definido en `pom.xml`) que añade `-XstartOnFirstThread`.

---

## 2. Diseño de la ciudad

| Constante | Valor | Significado |
|-----------|-------|-------------|
| `CELDA`   | 10    | Ancho y profundidad de cada celda (unidades OpenGL) |
| `LIMITE`  | 55    | Distancia del origen a cada borde; el mapa mide **110 × 110 unidades** |

**Mapa:** matriz `MAPA[11][11]` declarada en `clase1.java`. Códigos: `0` = calle, `1` = edificio, `2` = parque.

Conteo real de celdas (verificado fila a fila):

| Tipo | Cantidad |
|------|----------|
| Edificios (tipo 1) | **19** |
| Parques   (tipo 2) | **6**  |
| Total manzanas     | **25** |

**Red de calles:** las filas y columnas con índice par (0, 2, 4, 6, 8, 10) son siempre calle, formando una cuadrícula ortogonal continua de **6 avenidas horizontales × 6 avenidas verticales**. Las celdas de manzana ocupan únicamente índices impares en ambos ejes. Esta disposición garantiza que cualquier punto de calle esté conectado a todos los demás sin cortes, hecho verificado en `ConduccionTest.testDestinosAlcanzables()` mediante BFS sobre el MAPA.

---

## 3. Qué se adaptó al ampliar el mapa

| Aspecto | Archivo | Método |
|---------|---------|--------|
| **Suelo** — ancho `LIMITE*2` | `clase1.java` | `escena()` |
| **Límite de colisión** — `LIMITE - RADIO_AUTO` | `clase2.java` | `puedeCircular()` |
| **Colisiones con manzanas** — semiancho `±5` u. | `clase2.java` | `puedeCircular()` |
| **Cámara orbital** — radio `LIMITE*1.9`, altura `LIMITE*1.6` | `clase1.java` | `configurarCamara()` |
| **Minimapa** — divisor `LIMITE+2`; indicador `4×6` u. | `clase1.java` / `clase4.java` | `dibujarFrame()`, `dibujarIndicadorAuto()` |
| **Farolas** — 25 piezas generadas desde `MAPA` | `clase3.java` | `crearLuces()` |
| **Destinos** — cinco coordenadas en rango ±50 | `clase4.java` | constante `DESTINOS[][]` |
| **Semáforos** — posición desde `posicionFarola()` | `clase4.java` | `decorarCiudad()` → `dibujarSemaforo()` |
| **Cámara perspectiva** — plano lejano 400, cercano 0.2 | `clase1.java` | `vertexShader()` |
| **Tests** — umbrales ≥11 filas/cols, ≥12 edif., ≥4 parques, ≥9 farolas | `ConduccionTest.java` | `testMapaYConstantes()`, `testLuces()` |

---

## 4. Controles

| Tecla | Acción |
|-------|--------|
| `W` / Flecha arriba   | Acelerar |
| `S` / Flecha abajo    | Frenar / reversa |
| `A` / Flecha izquierda | Girar a la izquierda |
| `D` / Flecha derecha  | Girar a la derecha |
| `Espacio` | Freno de mano |
| `C` | Alternar cámara (seguimiento / aérea orbital) |
| `N` | Alternar día / noche |
| `F` | Encender / apagar faros del auto |
| `M` | Mostrar / ocultar minimapa |
| `R` | Reiniciar posición y contador de entregas |
| `ESC` | Salir |

---

## 5. Funciones implementadas (Requisitos obligatorios)

- **Iluminación día / noche** (`clase3.java`, `fragmentShader()`): de día, ambiente `0.48` + difusa solar `0.65`; de noche, ambiente azulado `(0.12, 0.16, 0.24)` + difusa `0.10`.
- **Farolas** (`clase3.java`, `crearLuces()`): **25 farolas**, una por manzana, a `Y = 4.5`. De noche emiten luz cálida `(1.0, 0.73, 0.34)` con atenuación cuadrática.
- **Faros del auto** (`clase3.java`, `fragmentShader()`): dos conos de luz frontales con `smoothstep(0.85, 0.97)`, atenuación cuadrática, color `(1.0, 0.94, 0.72)`.
- **Luz difusa con normales** (`clase1.java`, `vertexShader()`): cada vértice lleva su normal; el shader aplica Lambert por fragmento.
- **Decoración urbana obligatoria** (`clase4.java`, `decorarCiudad()`): árboles (tronco + copa) y bancos (asiento, soporte, respaldo) en parques, ventanas por pisos en las cuatro fachadas de edificios que parecen iluminadas de noche mediante emisión, y pasos peatonales cebrados sobre la calzada contigua.
- **Semáforos animados** (`clase4.java`, `dibujarSemaforo()`): ciclo de **12 s** — rojo 5 s (fases 0–4), verde 5 s (fases 5–9), amarillo 2 s (fases 10–11).
- **Minimapa** (`clase4.java`, `dibujarFrame()`): segundo pase ortográfico en un recuadro de hasta 260 px; indicador cian con punta blanca de dirección y destino activo, con el norte hacia arriba.
- **Reinicio** (`clase2.java` + `clase4.java`, `reiniciar()`): restaura posición `(−30, 30)`, ángulo `0`, velocidad `0`, entregas `0` y cronómetro `0`.
- **Entregas** (`clase4.java`, `actualizar()`): 5 paradas; se valida distancia < 3 u. y velocidad < 1 u/s. El título muestra progreso y tiempo.

---

## 6. Limitaciones y Mejoras Opcionales No Implementadas

- **Mejoras opcionales no implementadas:**
  - Sin **tráfico autónomo** ni otros vehículos (mejora opcional no implementada).
  - Sin **panel dentro de la ventana (HUD)** con velocidad y luces: la información se muestra en el título de la ventana (mejora opcional no implementada).
  - Sin **menú o pantalla de ayuda** dentro de la ventana (mejora opcional no implementada).
  - Los semáforos son solo visuales; el enunciado no exige que detengan el auto.
  - Sin señalización vertical adicional (mejora opcional no implementada).
- **Limitaciones gráficas y generales:**
  - Sin **sombras proyectadas**: iluminación local únicamente (no ray-tracing ni shadow maps).
  - Sin música ni efectos de sonido.

---

## 7. Recursos externos

- **Base del proyecto:** ejemplos de clase del docente (estructura de ventana, VAO/VBO, loop principal).
- **LWJGL 3.3.3:** lwjgl-core, lwjgl-glfw, lwjgl-opengl — declarados en `pom.xml`.
- **JUnit 3.8.1:** framework de pruebas — declarado en `pom.xml` como dependencia de test.
- **Asistencia de IA:** se utilizó asistencia de inteligencia artificial para apoyar la adaptación del código al mapa 11×11, la generación automática de farolas y la escritura de los tests.
