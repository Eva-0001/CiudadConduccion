# Ciudad y Conducción con OpenGL

Ciudad 3D interactiva en **Java 17 + LWJGL 3.3.3 (GLFW + OpenGL 3.3 Core)**. Un auto con **chata (remolque) acoplada** recorre una ciudad de 11×11 celdas con iluminación día/noche, semáforos animados, minimapa y un juego de 5 entregas. Toda la geometría es procedural (cubos transformados): no usa texturas ni modelos externos.

## Requisitos

- JDK 17 o superior (`java -version`)
- Apache Maven 3.x (`mvn -version`)
- GPU/driver con soporte OpenGL 3.3 Core

## Ejecutar

Desde la raíz del proyecto (donde está [`pom.xml`](pom.xml)):

```sh
mvn compile exec:exec        # versión final (JuegoEntregas)
mvn test                     # pruebas unitarias
mvn compile exec:exec -Ddemo.frames=10   # arranca, dibuja 10 cuadros y cierra (smoke test)
```

Cada etapa se puede ejecutar por separado:

```sh
mvn compile exec:exec -DmainClass=com.graphics.CiudadBase
mvn compile exec:exec -DmainClass=com.graphics.CiudadConAuto
mvn compile exec:exec -DmainClass=com.graphics.CiudadIluminada
mvn compile exec:exec -DmainClass=com.graphics.JuegoEntregas
```

> - Se usa `exec:exec` (no `exec:java`) para que GLFW corra en el hilo principal.
> - Binarios nativos incluidos: Windows x64 y Linux (x64/ARM64).


## Controles

| Tecla             | Acción                        |
| ----------------- | ----------------------------- |
| `W` / `↑`         | Acelerar                      |
| `S` / `↓`         | Frenar / reversa              |
| `A` `D` / `←` `→` | Girar                         |
| `Espacio`         | Freno de mano                 |
| `C`               | Cámara: seguimiento ↔ orbital |
| `N`               | Día / noche                   |
| `F`               | Faros del auto                |
| `M`               | Mostrar / ocultar minimapa    |
| `R`               | Reiniciar auto y entregas     |
| `ESC`             | Salir                         |

La barra de título muestra velocidad (km/h), estado de luces, entregas y cronómetro.

**Objetivo del juego:** llegar a cada una de las 5 paradas (a menos de 3 unidades) y detenerte (velocidad < 1 u/s). La parada activa se marca con una baliza dorada y en el minimapa.

## Arquitectura

Cada etapa extiende a la anterior (herencia), sin duplicar código:

```text
CiudadBase            Mapa 11×11, shaders, cubo con normales, cámara orbital
 └─ CiudadConAuto     Auto + chata acoplada, física, colisiones compuestas, cámara de seguimiento
     └─ CiudadIluminada   Día/noche, 25 farolas, faros cónicos, iluminación difusa
         └─ JuegoEntregas     Parques, ventanas, pasos peatonales, semáforos, minimapa, entregas
```

| Archivo                                                                   | Responsabilidad                                                                                                                                                 |
| ------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [`CiudadBase.java`](src/main/java/com/graphics/CiudadBase.java)           | GLFW, shaders, VAO/VBO del cubo unitario, matriz `MAPA`, `caja()` / `cajaGirada()`                                                                              |
| [`CiudadConAuto.java`](src/main/java/com/graphics/CiudadConAuto.java)     | Modelo del auto y `dibujarChataRemolque()`, movimiento, colisiones (`puedeCircular()`, `chataColisiona()`, `vehiculoPuedeCircular()`), cámara `C`, reinicio `R` |
| [`CiudadIluminada.java`](src/main/java/com/graphics/CiudadIluminada.java) | Fragment shader con luz ambiental + solar + puntuales, `crearLuces()`, faros con `smoothstep`                                                                   |
| [`JuegoEntregas.java`](src/main/java/com/graphics/JuegoEntregas.java)     | `decorarCiudad()`, `dibujarSemaforo()`, minimapa con `glScissor`, `DESTINOS`                                                                                    |
| [`ConduccionTest.java`](src/test/java/com/graphics/ConduccionTest.java)   | 7 pruebas JUnit sin contexto gráfico                                                                                                                            |

### Datos clave del mapa

- `MAPA[11][11]`: `0` = calle, `1` = edificio, `2` = parque.
- `CELDA = 10` unidades, `LIMITE = 55` → ciudad de 110×110.
- 19 edificios + 6 parques = 25 manzanas; filas/columnas pares son siempre calle (red conectada).
- Inicio del auto: `(-30, 30)`.

## Pruebas

`mvn test` ejecuta `ConduccionTest`:

| Prueba                    | Verifica                                                                                  |
| ------------------------- | ----------------------------------------------------------------------------------------- |
| `testMapaYConstantes`     | Mapa ≥ 11×11, `CELDA`, `LIMITE`, conteo de manzanas                                       |
| `testCallesTransitables`  | Avenidas y punto de inicio son transitables                                               |
| `testObstaculosYLimites`  | El auto no entra a edificios/parques ni sale del mapa                                     |
| `testDestinosAlcanzables` | BFS: las 5 paradas están conectadas al inicio                                             |
| `testLuces`               | Se generan 25 farolas dentro de la ciudad                                                 |
| `testReinicio`            | `reiniciar()` restablece posición, ángulo, velocidad y entregas                           |
| `testColisionChata`       | La chata y su lanza detectan edificios y bordes; el vehículo no circula si la chata choca |

## Limitaciones

- Sin tráfico autónomo ni menú/HUD dentro de la ventana (la info va en la barra de título).
- Los semáforos son visuales: no obligan al auto a frenar.
- Sin sombras arrojadas ni sonido.
