# Ciudad y Conducción — Documento de entrega

**Descripción general:** Conducción 3D interactiva con OpenGL 3.3 Core y Java 17. Un auto con **chata (remolque) acoplada** recorre una ciudad procedural de 11×11 celdas completando un recorrido de 5 entregas.

**Tecnología:** Java 17, Maven, LWJGL 3.3.3 (GLFW + OpenGL 3.3 Core). Plataformas soportadas: Windows x64 y Linux (x64/ARM64).

## 1. Ejecución

```bash
mvn compile exec:exec   # versión final (JuegoEntregas)
mvn test                # 7 pruebas unitarias (incluyendo testColisionChata)
```

Requiere JDK 17+ y Apache Maven.

## 2. Arquitectura por etapas y adaptaciones

Diseño modular con herencia progresiva:
- `CiudadBase`: mapa 11×11 (19 edificios + 6 parques = 25 manzanas, red de calles continua), suelo, shaders y cámara orbital.
- `CiudadConAuto`: cinemática de conducción, modelo del auto con **chata (remolque) acoplada**, colisiones compuestas y cámara de seguimiento.
- `CiudadIluminada`: ciclo día/noche, 25 farolas procedimentales y faros cónicos frontales.
- `JuegoEntregas`: decoración urbana, semáforos animados, minimapa ortográfico y 5 destinos de entrega.

Se adaptaron al nuevo mapa 11×11: suelo (`escena()`), colisiones y bordes (`puedeCircular()`, `chataColisiona()`), cámara orbital (`configurarCamara()`), minimapa (`dibujarFrame()`), farolas (`crearLuces()`), destinos (`DESTINOS`) y umbrales de tests en `ConduccionTest`.

## 3. Controles

`W/A/S/D` o flechas: conducir · `Espacio`: freno · `C`: cámara · `N`: día/noche · `F`: faros · `M`: minimapa · `R`: reiniciar · `ESC`: salir.

## 4. Funciones implementadas

- **Auto con chata (remolque) acoplada**: enganche, plataforma, ruedas y carga — `dibujarChataRemolque()`.
- **Colisiones compuestas**: prevención de impactos del auto y la chata contra manzanas y bordes — `puedeCircular()`, `chataColisiona()`, `vehiculoPuedeCircular()`.
- **Día/noche (`N`) y luz difusa**: normales por vértice y Lambert por fragmento — `CiudadIluminada.fragmentShader()`.
- **25 farolas procedimentales** con atenuación cuadrática — `crearLuces()`.
- **Faros del auto (`F`)**: dos conos frontales calculados con `smoothstep` — `fragmentShader()`.
- **Decoración urbana:** árboles, bancos, ventanas con emisión y pasos peatonales — `decorarCiudad()`.
- **Semáforos animados:** ciclo de 12 s (rojo 5 s, verde 5 s, amarillo 2 s) — `dibujarSemaforo()`.
- **Minimapa (`M`)**: render ortográfico cenital secundario con `glScissor` — `dibujarFrame()`.
- **Reinicio (`R`) y 5 entregas**: validación de distancia (< 3 u) y detención (< 1 u/s) — `reiniciar()`, `actualizar()`.

## 5. Pruebas y verificación

`mvn test` corre las 7 pruebas unitarias de `ConduccionTest` (mapa, calles transitables, obstáculos/límites, destinos BFS, farolas, reinicio y `testColisionChata`).

## 6. Limitaciones

No incluye mejoras opcionales (sin tráfico autónomo, ni HUD en ventana: la telemetría va en la barra de título). Los semáforos son decorativos. Sin sombras arrojadas ni audio.

## 7. Recursos

Ejemplos base (`AppCamara`, `AppLaberinto`), LWJGL 3.3.3 y JUnit 3.8.1. Se empleó asistencia de IA en estructuración, adaptación del mapa y pruebas.
