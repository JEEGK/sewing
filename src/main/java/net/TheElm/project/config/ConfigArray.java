/*
 * This software is licensed under the MIT License
 * https://github.com/GStefanowich/MC-Server-Protection
 *
 * Copyright (c) 2019 Gregory Stefanowich
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.TheElm.project.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public final class ConfigArray<T extends Object> extends ConfigBase<T> {
    
    private final Function<JsonElement, T> setter;
    private final List<T> value;
    private final boolean shuffle;
    
    public ConfigArray(@NotNull String location, Function<JsonElement, T> setter) {
        this(location, new ArrayList<>(), setter, false);
    }
    public ConfigArray(@NotNull String location, Function<JsonElement, T> setter, boolean shuffle) {
        this(location, new ArrayList<>(), setter, shuffle);
    }
    public ConfigArray(@NotNull String location, List<T> defaultValue, Function<JsonElement, T> setter) {
        this(location, defaultValue, setter, false);
    }
    public ConfigArray(@NotNull String location, List<T> defaultValue, Function<JsonElement, T> setter, boolean shuffle) {
        super(location);
        
        this.value = defaultValue;
        this.setter = setter;
        this.shuffle = shuffle;
    }
    
    @Override
    JsonElement getElement() {
        return new GsonBuilder()
            .disableHtmlEscaping().create().toJsonTree(this.value);
    }
    List<T> get() {
        return this.value;
    }
    T get(int index) {
        return this.value.get(index);
    }
    T getRandom() {
        if (this.value.size() == 1)
            return this.get(0);
        return this.get(ThreadLocalRandom.current().nextInt(this.value.size()));
    }
    @Override
    void set(JsonElement value) {
        // Reset values
        this.value.clear();
        
        // Do nothing if NULL
        if (value == null) return;
        
        // Add all values
        if (!(value instanceof JsonArray))
            this.value.add(this.setter.apply(value));
        else for (JsonElement element : value.getAsJsonArray())
            this.value.add(this.setter.apply(element));
        
        // Shuffle the array
        if (this.shuffle)
            Collections.shuffle(this.value);
    }
    
    public static @NotNull ConfigArray<Integer> jInt(@NotNull String location) {
        return new ConfigArray<>(location, JsonElement::getAsInt);
    }
    public static @NotNull ConfigArray<Long> jLong(@NotNull String location) {
        return new ConfigArray<>(location, JsonElement::getAsLong);
    }
    public static @NotNull ConfigArray<Boolean> jBool(@NotNull String location) {
        return new ConfigArray<>(location, JsonElement::getAsBoolean);
    }
    public static @NotNull ConfigArray<String> jString(@NotNull String location) {
        return new ConfigArray<>(location, JsonElement::getAsString);
    }
    public static @NotNull ConfigArray<String> jString(@NotNull String location, boolean shuffled) {
        return new ConfigArray<>(location, JsonElement::getAsString, shuffled);
    }
}
