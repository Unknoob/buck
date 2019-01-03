/*
 * Copyright 2016-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.cxx.toolchain.linker;

import com.facebook.buck.core.model.BuildTarget;
import com.facebook.buck.core.rules.BuildRuleResolver;
import com.facebook.buck.core.toolchain.tool.Tool;
import com.facebook.buck.core.toolchain.toolprovider.ToolProvider;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import javax.annotation.Nonnull;

public class DefaultLinkerProvider implements LinkerProvider {

  private final Type type;
  private final ToolProvider toolProvider;
  private final boolean cacheLinks;

  private final LoadingCache<BuildRuleResolver, Linker> cache =
      CacheBuilder.newBuilder()
          .weakKeys()
          .build(
              new CacheLoader<BuildRuleResolver, Linker>() {
                @Override
                public Linker load(@Nonnull BuildRuleResolver resolver) {
                  return build(type, toolProvider.resolve(resolver), cacheLinks);
                }
              });

  public DefaultLinkerProvider(Type type, ToolProvider toolProvider, boolean cacheLinks) {
    this.type = type;
    this.toolProvider = toolProvider;
    this.cacheLinks = cacheLinks;
  }

  private static Linker build(Type type, Tool tool, boolean cacheLinks) {
    switch (type) {
      case DARWIN:
        return new DarwinLinker(tool, cacheLinks);
      case GNU:
        return new GnuLinker(tool);
      case WINDOWS:
        return new WindowsLinker(tool);
      case UNKNOWN:
      default:
        throw new IllegalStateException("unexpected type: " + type);
    }
  }

  @Override
  public synchronized Linker resolve(BuildRuleResolver resolver) {
    return cache.getUnchecked(resolver);
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public Iterable<BuildTarget> getParseTimeDeps() {
    return toolProvider.getParseTimeDeps();
  }
}
