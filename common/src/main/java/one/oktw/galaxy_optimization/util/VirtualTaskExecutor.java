/*
 * OKTW Galaxy Optimization
 * Copyright (C) 2026-2026
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package one.oktw.galaxy_optimization.util;

import net.minecraft.Util;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.util.thread.StrictQueue;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class VirtualTaskExecutor<T> extends ProcessorMailbox<T> {
    private final ExecutorService executor;
    private final StrictQueue<? super T, ? extends Runnable> queue;
    private final AtomicInteger executePriority = new AtomicInteger(0);
    private final AtomicInteger executingTask = new AtomicInteger(0);

    public VirtualTaskExecutor(StrictQueue<? super T, ? extends Runnable> queue, String name) {
        super(queue, null, name);
        executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name(name, 0).factory());
        this.queue = queue;
    }

    @Override
    public void tell(@NotNull T message) {
        queue.push(message);
        executor.execute(this::runTasks);
    }

    private void runTasks() {
        while (!queue.isEmpty()) {
            StrictQueue.IntRunnable task = (StrictQueue.IntRunnable) queue.pop();
            if (task == null) continue;

            // Check task priority
            if (executingTask.get() > 0 && task.getPriority() > executePriority.get()) {
                // executing task priority higher than next task, wait all task done
                queue.push((T) task);
                break;
            }

            // Run task
            executePriority.set(task.getPriority());
            executingTask.incrementAndGet();
            executor.execute(() -> {
                Util.wrapThreadWithTaskName(name(), task).run();
                if (executingTask.decrementAndGet() <= 0) runTasks(); // Trigger next write batch
            });
        }
    }

    @Override
    public void close() {
        executor.close();
    }
}
