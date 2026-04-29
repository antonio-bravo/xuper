

Voy a buscar en el código cómo obtiene los canales y la parrilla de programación:
 

Searched for text `http` (`XPR_Tv _thexupertv.com4.34.3.apk` · `apk_descompilado/sources/**/*.java`), 30 results




Esos son solo librerías. Déjame buscar en el código específico de la app (paquete bone):
 

Searched for text `url` (`XPR_Tv _thexupertv.com4.34.3.apk` · `apk_descompilado/sources/com/bone/**/*.java`), 30 results



Veo que hay referencias a URLs. Déjame buscar más específicamente sobre canales y listas de reproducción:
 

Searched for text `channel canal stream` (`XPR_Tv _thexupertv.com4.34.3.apk` · `apk_descompilado/sources/com/bone/**/*.java`), no results




Déjame buscar en los recursos (strings, donde suelen estar las URLs de los servidores):
 

Searched for files matching `XPR_Tv _thexupertv.com4.34.3.apk` · `apk_descompilado/resources/res/values/*.xml`, 10 matches




 

Read [](file:///Users/antonio-bravo/Downloads/XPR_Tv%20_thexupertv.com4.34.3.apk/apk_descompilado/resources/res/values/strings.xml)



¡Excelente! Encontré información clave. Déjame buscar más detalles sobre la generación de la parrilla:
 

Searched for text `guia agenda schedule event` (`XPR_Tv _thexupertv.com4.34.3.apk` · `apk_descompilado/sources/com/bone/**/*.java`), no results




Déjame buscar más en los recursos:
 

Searched for text `guia agenda` (`XPR_Tv _thexupertv.com4.34.3.apk` · `apk_descompilado/resources/**/*.xml`), no results




Déjame buscar en el código Java por las clases principales:
 

Searched for files matching `XPR_Tv _thexupertv.com4.34.3.apk` · `apk_descompilado/sources/com/bone/android/a4v/oficial/*.java`, 7 matches




¡Perfecto! Encontré las clases principales. Déjame leer el código:
 

Read [](file:///Users/antonio-bravo/Downloads/XPR_Tv%20_thexupertv.com4.34.3.apk/apk_descompilado/sources/com/bone/android/a4v/oficial/AVActivity.java#1-1), lines 1 to 150




Veo que hay variables importantes. Déjame buscar más en el código para encontrar cómo carga los canales:
 

Searched for text `livelink arenavision arena` (`XPR_Tv _thexupertv.com4.34.3.apk` · `apk_descompilado/sources/com/bone/**/*.java`), no results




 

Searched for text `livelink` (`XPR_Tv _thexupertv.com4.34.3.apk` · `apk_descompilado/sources/com/bone/**/*.java`), 5 results




Encontré referencias clave. Déjame ver la clase AVCHActivity que maneja los canales:
 

Read [](file:///Users/antonio-bravo/Downloads/XPR_Tv%20_thexupertv.com4.34.3.apk/apk_descompilado/sources/com/bone/android/a4v/oficial/AVCHActivity.java#1-1), lines 1 to 100




Veo que los streams vienen de un Intent. Déjame buscar más en el código para ver cómo se genera la lista:
 

Searched for text `streams` (`XPR_Tv _thexupertv.com4.34.3.apk` · `apk_descompilado/sources/com/bone/**/*.java`), 30 results



¡Excelente! Encontré el método clave. Déjame ver cómo extrae los streams:
 

Read [](file:///Users/antonio-bravo/Downloads/XPR_Tv%20_thexupertv.com4.34.3.apk/apk_descompilado/sources/com/bone/android/a4v/oficial/AVActivity.java#1760-1760), lines 1760 to 1850



Ahora necesito ver cómo se genera la agenda de eventos. Déjame buscar más:
 

Searched for text `evento evento` (`XPR_Tv _thexupertv.com4.34.3.apk` · `apk_descompilado/sources/com/bone/**/*.java`), no results




Veamos la clase Evento:
 

Read [](file:///Users/antonio-bravo/Downloads/XPR_Tv%20_thexupertv.com4.34.3.apk/apk_descompilado/sources/com/bone/android/a4v/oficial/Evento.java)




Ahora busquemos cómo se carga la agenda desde el servidor:
 

Searched for text `laagenda` (`XPR_Tv _thexupertv.com4.34.3.apk` · `apk_descompilado/sources/com/bone/**/*.java`), 1 result




 

Searched for text `carga agenda` (`XPR_Tv _thexupertv.com4.34.3.apk` · `apk_descompilado/sources/com/bone/**/*.java`), no results




 

Searched for text `eventosDeldia` (`XPR_Tv _thexupertv.com4.34.3.apk` · `apk_descompilado/sources/com/bone/**/*.java`), 10 results




Veamos cómo se parsea la agenda:
 

Read [](file:///Users/antonio-bravo/Downloads/XPR_Tv%20_thexupertv.com4.34.3.apk/apk_descompilado/sources/com/bone/android/a4v/oficial/AVActivity.java#1020-1020), lines 1020 to 1100



Ahora necesito ver de dónde viene la agenda. Busquemos la llamada a `sacaAgenda`:
 

Searched for text `sacaAgenda` (`XPR_Tv _thexupertv.com4.34.3.apk` · `apk_descompilado/sources/com/bone/**/*.java`), 9 results




Veamos cómo se carga el contenido de la página web:
 

Searched for text `tope =` (`XPR_Tv _thexupertv.com4.34.3.apk` · `apk_descompilado/sources/com/bone/**/*.java`), 9 results




Veo que hay un método `loadAGENDA()`. Déjame buscarlo:
 

Searched for text `loadAGENDA` (`XPR_Tv _thexupertv.com4.34.3.apk` · `apk_descompilado/sources/com/bone/**/*.java`), 9 results




Veamos el método `loadAGENDA`:
 

Read [](file:///Users/antonio-bravo/Downloads/XPR_Tv%20_thexupertv.com4.34.3.apk/apk_descompilado/sources/com/bone/android/a4v/oficial/AVActivity.java#1425-1425), lines 1425 to 1500




Veo que la agenda se carga desde un archivo local o preferencias. Déjame buscar cómo se descarga la agenda:
 

Searched for text `lOLF` (`XPR_Tv _thexupertv.com4.34.3.apk` · `apk_descompilado/sources/com/bone/**/*.java`), 4 results




 

Read [](file:///Users/antonio-bravo/Downloads/XPR_Tv%20_thexupertv.com4.34.3.apk/apk_descompilado/sources/com/bone/android/a4v/oficial/AVActivity.java#1985-1985), lines 1985 to 2050



Ahora tengo toda la información. Déjame darte un resumen completo:

---

## 📡 Cómo funciona Arena4Viewer

### 1. Lista de Canales

La app obtiene los canales desde **servidores web** externos. Los URLs principales están hardcodeados en los recursos:

| Dominio | URL |
|---------|-----|
| Principal | `http://www.arena4viewer.in/misguia2.php` |
| Alternativo 1 | `http://www.arena4viewer.pl/misguia2.php` |
| Alternativo 2 | `https://www.arena4viewer.co.in/misguia2.php` |
| Alternativo 3 | `https://www.arena4viewer.cool/misguia2.php` |
| Alternativo 4 | `https://www.arena4viewer.lv/misguia2.php` |
| Alternativo 5 | `https://www.arena4viewer.top/misguia2.php` |

**Proceso:**
1. La app descarga un archivo HTML desde uno de estos servidores
2. Extrae los streams de un div oculto: `<div class"streams" style="display:none;">`
3. Los parsea y los guarda en memoria (`miStreams`)
4. Soporta 3 tipos de streams:
   - **ACESTREAM** (`acestream://`)
   - **SOPCAST** (`sop://`)
   - **WWW** (HTTP/HTTPS directo)

### 2. Parrilla de Eventos (Agenda)

La agenda se obtiene de la **misma fuente** (`misguia2.php`):

**Proceso:**
1. Descarga el HTML completo del servidor
2. Extrae una tabla HTML (`<table>...</table>`)
3. Parsea cada fila (`<tr>`) extrayendo:
   - **Fecha/Hora** (ej: `29/04/2026 20:00`)
   - **Deporte** (Fútbol, Tenis, Motos, etc.)
   - **Evento/Encuentro** (ej: "Real Madrid vs Barcelona")
   - **Competición** (Liga Champions, ATP, etc.)
   - **Canales disponibles** (AV1, AV2, AV3...)

4. Los guarda en un archivo local (`a4v.lic`) codificado en **Base64**
5. Los carga desde caché local o preferencias compartidas

### 3. Estructura de datos

```java
// Modelo de evento
class Evento {
    String fyh;        // Fecha y hora
    String deporte;    // Fútbol, Tenis, Motos...
    String encuentro; // Equipo1 vs Equipo2
    String competicion; // Liga, Copa, etc.
    String canales;    // AV1, AV2, AV3...
}
```

### 4. URLs de canales de streaming

Los streams se cargan desde:
- **LiveFootballol.me**: `http://www.livefootballol.me/channels/`
- **ArenaV**: `http://arenav.bget.ru/`
- **Servidores alternativos**: `http://es.live3s.com/`

---

### ⚠️ Nota importante

Esta app accede a contenido que puede violar derechos de autor. Los servidores y URLs cambian frecuentemente para evadir bloqueos. El código está ofuscado para proteger la lógica de negocio.