package com.centralcore.util;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.BaseTSD.LONG_PTR;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import javafx.application.Platform;
import javafx.stage.Stage;

//íntegra la ventana de javafx con el dwm de windows para resize nativo y sombra, sin barra nativa
public class DwmManager {

    //mensajes de windows manejados por el wndproc
    private static final int WM_NCHITTEST  = 0x0084;
    private static final int WM_NCCALCSIZE = 0x0083;

    //resultados del hit test para resize en cada borde y esquina
    private static final int HTCLIENT = 1;
    private static final int HTLEFT = 10;
    private static final int HTRIGHT = 11;
    private static final int HTTOP = 12;
    private static final int HTTOPLEFT = 13;
    private static final int HTTOPRIGHT = 14;
    private static final int HTBOTTOM = 15;
    private static final int HTBOTTOMLEFT = 16;
    private static final int HTBOTTOMRIGHT = 17;

    //grosor del borde de resize en pixeles
    private static final int BORDER = 6;

    //estilo de ventana con borde grueso, necesario para que windows gestione el resize nativo
    private static final int WS_THICKFRAME = 0x00040000;

    //indices de SetWindowLong/GetWindowLong
    private static final int GWL_STYLE = -16;
    private static final int GWL_WNDPROC = -4;

    //flags para SetWindowPos al forzar recálculo del frame
    private static final int SWP_FRAMECHANGED = 0x0020;
    private static final int SWP_NOMOVE = 0x0002;
    private static final int SWP_NOSIZE = 0x0001;
    private static final int SWP_NOZORDER = 0x0004;

    //interfaz jna para acceder a dwmapi.dll
    interface Dwmapi extends StdCallLibrary {

        Dwmapi INSTANCE = Native.load(
                "dwmapi",
                Dwmapi.class,
                W32APIOptions.DEFAULT_OPTIONS
        );

        int DwmExtendFrameIntoClientArea(HWND hwnd, MARGINS margins);
    }

    //estructura margins requerida por dwm
    public static class MARGINS extends Structure {

        public int cxLeftWidth;
        public int cxRightWidth;
        public int cyTopHeight;
        public int cyBottomHeight;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(
                    "cxLeftWidth",
                    "cxRightWidth",
                    "cyTopHeight",
                    "cyBottomHeight"
            );
        }
    }

    //interfaz jna extendida para subclassing del wndproc y manipulación de estilos
    interface User32Ex extends StdCallLibrary {

        User32Ex INSTANCE = Native.load(
                "user32",
                User32Ex.class,
                W32APIOptions.DEFAULT_OPTIONS
        );

        //leer el estilo actual de la ventana
        int GetWindowLong(HWND hwnd, int index);

        //escribir un nuevo estilo entero
        int SetWindowLong(HWND hwnd, int index, int value);

        //subclasificar el wndproc con una función java
        LONG_PTR SetWindowLongPtr(HWND hwnd, int index, WinUser.WindowProc wndProc);

        //reenviar mensajes al proc original
        LRESULT CallWindowProc(LONG_PTR previousProc, HWND hwnd, int message, WPARAM wParam, LPARAM lParam);

        boolean SetWindowPos(HWND hwnd, HWND insertAfter, int x, int y, int width, int height, int flags);
    }

    //instala la integración dwm en la ventana de javafx
    public static void install(Stage stage) {

        //solo ejecutar en windows
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) return;

        //esperar a que la ventana exista realmente antes de pedir el hwnd
        Platform.runLater(() -> {
            try {
                HWND hwnd = getHwnd(stage);
                if (hwnd == null) return;

                //añadir WS_THICKFRAME para activar el resize nativo de windows
                addThickFrame(hwnd);

                //extender el frame dwm para que windows dibuje sombra y borde accent
                extendFrame(hwnd);

                //subclasificar el wndproc para hit testing personalizado
                subclassWindowProc(hwnd, stage);

                //forzar a windows a recalcular el frame con el nuevo estilo
                User32Ex.INSTANCE.SetWindowPos(hwnd, null, 0, 0, 0, 0,
                        SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED);

            } catch (Throwable t) {
                //si falla jna o alguna llamada dwm simplemente continuar normal
                System.err.println("DwmManager: no se pudo instalar la integracion dwm: " + t.getMessage());
            }
        });
    }

    //añade WS_THICKFRAME al estilo de la ventana para que windows gestione el resize nativo
    private static void addThickFrame(HWND hwnd) {
        int currentStyle = User32Ex.INSTANCE.GetWindowLong(hwnd, GWL_STYLE);
        User32Ex.INSTANCE.SetWindowLong(hwnd, GWL_STYLE, currentStyle | WS_THICKFRAME);
    }

    //extiende el frame dwm para que windows dibuje sombra y borde accent nativo
    private static void extendFrame(HWND hwnd) {
        MARGINS margins = new MARGINS();
        //-1 en todos los márgenes extiende el frame por toda la ventana
        margins.cxLeftWidth = -1;
        margins.cxRightWidth = -1;
        margins.cyTopHeight = -1;
        margins.cyBottomHeight = -1;
        Dwmapi.INSTANCE.DwmExtendFrameIntoClientArea(hwnd, margins);
    }

    //subclass del wndproc para resize nativo y supresión del frame no-cliente
    private static void subclassWindowProc(HWND hwnd, Stage stage) {

        //guardar referencia al proc original para mensajes no manejados
        LONG_PTR[] originalProc = new LONG_PTR[1];

        WinUser.WindowProc newProc = (hWnd, message, wParam, lParam) -> {

            //suprimir el área no-cliente para que windows no dibuje el marco grueso visualmente
            //el resize sigue funcionando porque WM_NCHITTEST lo gestiona desde nuestra lógica
            if (message == WM_NCCALCSIZE && wParam.intValue() == 1) {
                return new LRESULT(0);
            }

            if (message == WM_NCHITTEST) {

                //extraer posición del cursor desde lparam
                int lp = lParam.intValue();
                int curX = (short) (lp & 0xFFFF);
                int curY = (short) ((lp >> 16) & 0xFFFF);

                //obtener rectángulo actual de la ventana
                com.sun.jna.platform.win32.WinDef.RECT rect = new com.sun.jna.platform.win32.WinDef.RECT();
                User32.INSTANCE.GetWindowRect(hWnd, rect);

                int wLeft = rect.left;
                int wTop = rect.top;
                int wRight = rect.right;
                int wBottom = rect.bottom;

                boolean onLeft = curX < wLeft + BORDER;
                boolean onRight = curX > wRight - BORDER;
                boolean onTop = curY < wTop + BORDER;
                boolean onBottom = curY > wBottom - BORDER;

                if (onTop && onLeft)  return new LRESULT(HTTOPLEFT);
                if (onTop && onRight) return new LRESULT(HTTOPRIGHT);
                if (onBottom && onLeft)  return new LRESULT(HTBOTTOMLEFT);
                if (onBottom && onRight) return new LRESULT(HTBOTTOMRIGHT);
                if (onLeft) return new LRESULT(HTLEFT);
                if (onRight) return new LRESULT(HTRIGHT);
                if (onTop) return new LRESULT(HTTOP);
                if (onBottom) return new LRESULT(HTBOTTOM);

                //todo lo que no es borde es area cliente: javafx gestiona el drag y los botones
                //no devolver HTCAPTION porque windows entraría en el bucle modal de arrastre
                //y bloquearía los eventos de click de los botones de la barra
                return new LRESULT(HTCLIENT);
            }

            //reenviar mensajes no manejados al proc original
            return User32Ex.INSTANCE.CallWindowProc(originalProc[0], hWnd, message, wParam, lParam);
        };

        originalProc[0] = User32Ex.INSTANCE.SetWindowLongPtr(hwnd, GWL_WNDPROC, newProc);

        //guardar referencia fuerte para evitar que gc elimine el callback mientras la ventana existe
        stage.getProperties().put("dwmWindowProc", newProc);
    }

    //obtiene el hwnd nativo de la ventana javafx por titulo
    private static HWND getHwnd(Stage stage) {
        try {
            HWND hwnd = User32.INSTANCE.FindWindow(null, stage.getTitle());
            if (hwnd == null) {
                System.err.println("DwmManager: no se encontró la ventana con título: " + stage.getTitle());
            }
            return hwnd;
        } catch (Exception e) {
            System.err.println("DwmManager: error obteniendo hwnd: " + e.getMessage());
            return null;
        }
    }
}