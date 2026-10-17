# Ciudad y conducción con OpenGL: Arquitectura por etapas

Proyecto de gráficos por computadora y enseñanza de OpenGL moderno en Java: Java 17+, Maven, LWJGL 3.3.3, GLFW, OpenGL 3.3 Core, shaders GLSL, VAO y VBO. Toda la geometría se genera proceduralmente con código (cubos transformados); no se requieren imágenes de textura ni modelos 3D externos.

---

## Cómo ejecutar

Abre una terminal en la raíz del proyecto (`CiudadConduccion`), donde se ubica el archivo [pom.xml](file:///c:/Users/LENOVO/Documents/proyecto_grafica/CiudadConduccion/pom.xml). Se requiere un JDK 17 o superior y Apache Maven (`java -version`, `mvn -version`).

### Ejecución directa de la versión completa (por defecto)

El proyecto tiene configurada la clase final `JuegoEntregas` como `mainClass` en el [pom.xml](file:///c:/Users/LENOVO/Documents/proyecto_grafica/CiudadConduccion/pom.xml):

```sh
mvn compile exec:exec
```

### Ejecución por etapas individuales

Cada etapa posee su propio método `main` y puede ejecutarse de manera independiente para observar la progresión técnica:

```sh
# Etapa 1: Ciudad base, mapa 11x11 y cámara orbital
mvn compile exec:exec -DmainClass=com.graphics.CiudadBase

# Etapa 2: Auto con chata/remolque acoplado, físicas y colisiones
mvn compile exec:exec -DmainClass=com.graphics.CiudadConAuto

# Etapa 3: Iluminación día/noche, farolas urbanas y faros del vehículo
mvn compile exec:exec -DmainClass=com.graphics.CiudadIluminada

# Etapa 4: Decoración urbana, semáforos, minimapa y juego de entregas
mvn compile exec:exec -DmainClass=com.graphics.JuegoEntregas
```

> [!NOTE]
> - Ejecuta un comando por vez. Presiona **ESC** para cerrar la ventana.
> - Se utiliza `exec:exec` (y no `exec:java`) para asegurar la correcta inicialización del hilo principal en todas las plataformas.
> - En **macOS**, el perfil Maven activa automáticamente `-XstartOnFirstThread` para GLFW.
> - Se incluyen binarios nativos para Windows x64, macOS (Intel y Apple Silicon) y Linux (x64 y ARM64). Requiere soporte para OpenGL 3.3 Core Profile.

---

## Arquitectura y acumulación de avances

El proyecto se estructura bajo el principio de reutilización orientada a objetos mediante herencia (`extends`) y llamadas a `super`:

```text
CiudadBase
  │   (Mapa matricial 11×11, renderizador OpenGL 3.3, shaders básicos, cámara orbital)
  │
  └── CiudadConAuto extends CiudadBase
        │   (Física de vehículo, remolque/chata acoplada, colisiones compuestas y cámara de seguimiento)
        │
        └── CiudadIluminada extends CiudadConAuto
              │   (Ciclo día/noche, 25 farolas procedimentales, faros direccionales y shaders con iluminación Lambertiana)
              │
              └── JuegoEntregas extends CiudadIluminada
                    (Decoración de parques/edificios, semáforos animados, minimapa ortográfico con glScissor y juego de 5 entregas)
```

Cada clase extiende la funcionalidad de la anterior sin duplicar código:
- [CiudadBase.java](file:///c:/Users/LENOVO/Documents/proyecto_grafica/CiudadConduccion/src/main/java/com/graphics/CiudadBase.java): Configura GLFW, compila shaders, genera el cubo maestro unitario con normales y renderiza la cuadrícula urbana.
- [CiudadConAuto.java](file:///c:/Users/LENOVO/Documents/proyecto_grafica/CiudadConduccion/src/main/java/com/graphics/CiudadConAuto.java): Agrega la cinemática de aceleración, frenado, giro proporcional y el ensamblaje modular del vehículo con su chata acoplada, junto con la detección de colisiones de ambos componentes.
- [CiudadIluminada.java](file:///c:/Users/LENOVO/Documents/proyecto_grafica/CiudadConduccion/src/main/java/com/graphics/CiudadIluminada.java): Reemplaza el shader de color plano por cálculo de iluminación difusa, luces puntuales de farolas en cada manzana y conos de luz frontales de los faros con `smoothstep`.
- [JuegoEntregas.java](file:///c:/Users/LENOVO/Documents/proyecto_grafica/CiudadConduccion/src/main/java/com/graphics/JuegoEntregas.java): Agrega bancos y árboles en parques, ventanas en fachadas, pasos de peatones, semáforos animados de 3 fases, segundo pase de renderizado para minimapa con recorte (`glScissor`) y lógica de metas/entregas.

---

## Guion didáctico de las cuatro etapas

### Etapa 1 — `CiudadBase`: Ciudad 3D y renderizador básico

- **Resultado:** Una ciudad 3D con avenidas continuas, manzanas de edificios y parques, y cámara orbital controlable con flechas izquierda/derecha.
- **Conceptos clave:**
  1. **Matriz `MAPA` (11×11):** Donde `0` = calle, `1` = edificio y `2` = parque.
  2. **Coordenadas de mundo (`centro()`):** Cada celda mide `CELDA = 10` unidades; con 11 celdas el mapa abarca de `-55` a `+55` (`LIMITE = 55`, 110×110 unidades en total).
  3. **VBO y VAO:** Cubo unitario de 36 vértices con posiciones y normales explícitas.
  4. **Matrices y transformaciones:** Funciones `caja()` y `cajaGirada()` que multiplican traslación, rotación y escalado para renderizar cualquier estructura urbana.
  5. **Ciclo de vida de la ventana:** `iniciar()`, `loop()`, `dibujarFrame()` y `limpiar()`.

### Etapa 2 — `CiudadConAuto`: Vehículo, remolque ("chata") y colisiones

- **Resultado:** Vehículo rojo con cabina azulada y un remolque/chata de carga acoplado detrás con barandas de madera y encomiendas; cámara de seguimiento en tercera persona y física de conducción con prevención de choques.
- **Conceptos clave:**
  1. **Modelo compuesto del vehículo y la chata:** Ensamblado con transformaciones locales en `pieza()` y `dibujarChataRemolque()`: enganche, lanza en V, plataforma, barandas, esquineros metálicos, ruedas con llantas, guardabarros, luces y cajas de carga.
  2. **Cinemática del auto:** Integración de aceleración (`9 m/s²`), resistencia exponencial, reversa (`-6 m/s`) y avance máximo (`16 m/s`). El giro responde a la orientación y velocidad del vehículo.
  3. **Sistema de colisiones compuestas:**
     - `puedeCircular()`: Verifica que el círculo de colisión del auto no invada los rectángulos de las manzanas ni traspase los bordes de la ciudad.
     - `chataColisiona()`: Muestreo a lo largo del remolque (lanza, frente, centro/ruedas, sector posterior y portón) para impedir que la chata golpee aceras, edificios o rebase los bordes del mapa al girar.
     - `vehiculoPuedeCircular()`: Convalida conjuntamente auto y chata antes de autorizar el desplazamiento o la rotación.
  4. **Cámara:** Alternancia con tecla `C` entre cámara de seguimiento en tercera persona y cámara orbital general.

### Etapa 3 — `CiudadIluminada`: Ciclo día/noche y fuentes de luz

- **Resultado:** Farolas cálidas en las 25 manzanas urbanas, cambio instantáneo entre día y noche, y faros delanteros con atenuación cuadrática y proyección cónica.
- **Conceptos clave:**
  1. **Modelo de iluminación Phong/Lambert:** Shader de fragmentos con componente ambiental, solar direccional y atenuación por distancia.
  2. **25 Farolas procedimentales:** `crearLuces()` recorre la matriz y ubica una farola en la esquina exterior de cada manzana a `Y = 4.5`.
  3. **Faros delanteros cónicos:** Dos proyectores frontales calculados mediante producto punto (`dot`) y `smoothstep(0.85, 0.97)` para simular haces de luz realistas.
  4. **Emisión de materiales (`uEmision`):** Evita que las bombillas encendidas se oscurezcan de noche al evaluarse como fuentes de luz.

### Etapa 4 — `JuegoEntregas`: Detalles urbanos, entregas y minimapa

- **Resultado:** Paisaje urbano ambientado con árboles, bancos, ventanas, pasos peatonales cebrados y semáforos animados; minimapa en esquina superior derecha y dinámica de juego con 5 paradas de entrega.
- **Conceptos clave:**
  1. **Decoración urbana:** Parques con vegetación y bancos; edificios con ventanas emisivas calculadas por pisos en sus 4 fachadas; pasos peatonales en las calzadas adyacentes.
  2. **Semáforos animados:** Ciclo semafórico continuo de 12 segundos (rojo 5 s, verde 5 s, amarillo 2 s) distribuido en esquinas libres.
  3. **Mecánica de entregas:** 5 coordenadas (`DESTINOS`) donde el jugador debe posicionarse a menos de 3 unidades y frenar a menos de 1 u/s. La parada activa exhibe una plataforma y una baliza dorada giratoria.
  4. **Minimapa en segundo pase:** Renderizado ortográfico cenital en un viewport secundario con prueba de tijera (`glScissor`), mostrando la silueta del auto en cian, su vector de dirección en blanco y la meta dorada.

---

## Controles del juego

| Tecla | Acción | Disponible desde |
|---|---|---|
| **W** / **Flecha arriba** | Acelerar hacia adelante | `CiudadConAuto` (Etapa 2) |
| **S** / **Flecha abajo** | Frenar y dar marcha atrás | `CiudadConAuto` (Etapa 2) |
| **A** / **Flecha izquierda** | Girar a la izquierda (según velocidad) | `CiudadConAuto` (Etapa 2) |
| **D** / **Flecha derecha** | Girar a la derecha (según velocidad) | `CiudadConAuto` (Etapa 2) |
| **Espacio** | Freno de mano | `CiudadConAuto` (Etapa 2) |
| **C** | Alternar cámara (seguimiento / orbital general) | `CiudadConAuto` (Etapa 2) |
| **R** | Reiniciar vehículo (y contador de entregas en etapa 4) | `CiudadConAuto` (Etapa 2) |
| **N** | Alternar ciclo Día / Noche | `CiudadIluminada` (Etapa 3) |
| **F** | Encender / apagar faros frontales del auto | `CiudadIluminada` (Etapa 3) |
| **M** | Mostrar / ocultar minimapa | `JuegoEntregas` (Etapa 4) |
| **Flecha izquierda / derecha** | Orbitar cámara de la ciudad | `CiudadBase` (únicamente Etapa 1) |
| **ESC** | Salir de la aplicación | Todas las etapas |

> La barra de título de la ventana muestra en tiempo real la velocidad en km/h, el estado de iluminación, el avance de las entregas y el cronómetro de la partida.

---

## Verificación y pruebas automáticas

El proyecto incluye pruebas unitarias completas bajo JUnit que validan la geometría, rutas, colisiones y estado del juego sin requerir contexto gráfico:

```sh
mvn test
```

### Conjunto de pruebas ([ConduccionTest.java](file:///c:/Users/LENOVO/Documents/proyecto_grafica/CiudadConduccion/src/test/java/com/graphics/ConduccionTest.java))

1. **`testMapaYConstantes`**: Verifica dimensiones mínimas del mapa (11×11), tamaño de celda (`CELDA = 10`), cálculo de límites (`LIMITE = 55`) y conteo de manzanas (19 edificios y 6 parques).
2. **`testCallesTransitables`**: Comprueba que la avenida longitudinal oeste y la perimetral norte sean transitables de punta a punta, así como el punto de inicio `(-30, 30)`.
3. **`testObstaculosYLimites`**: Valida que el vehículo no pueda ingresar a centros de edificios, parques ni rebasar los bordes del mapa.
4. **`testDestinosAlcanzables`**: Mediante una búsqueda en anchura (BFS) sobre la red de calles, asegura que las 5 paradas de entrega estén efectivamente conectadas al punto de inicio.
5. **`testLuces`**: Comprueba que se generen las 25 farolas y que todas se ubiquen dentro de los márgenes de la ciudad.
6. **`testReinicio`**: Asegura que el método `reiniciar()` restablezca con precisión las coordenadas `(-30, 30)`, velocidad `0`, ángulo `0` y contador de entregas `0`.
7. **`testColisionChata`**: Valida la detección de colisiones de la chata/remolque y su lanza contra obstáculos y bordes exteriores, impidiendo que el vehículo circule si el acoplado choca.

### Parámetro de prueba visual breve

Para comprobar el enlace de shaders, carga de GLFW y contexto OpenGL sin dejar la ventana abierta indefinidamente, puede especificarse el argumento `-Ddemo.frames`:

```sh
mvn compile exec:exec -Ddemo.frames=10
```
La aplicación correrá durante 10 cuadros y se cerrará automáticamente, reportando errores de OpenGL si los hubiera.
