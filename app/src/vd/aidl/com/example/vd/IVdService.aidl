package com.example.vd;
import android.view.Surface;
interface IVdService {
    void destroy() = 16777114; // Shizuku reserved transaction code
    int createDisplay(String name, in Surface surface, int w, int h, int dpi) = 1;
    void resize(int id, int w, int h, int dpi) = 2;
    void release(int id) = 3;
    int launch(int id, String component) = 4;
    void touch(int id, int action, float x, float y, long downTime) = 5;
}
