/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.compute.functions;

import org.jclouds.compute.internal.FormatSharedNamesAndAppendUniqueStringToThoseWhichRepeat;
import org.jclouds.javax.annotation.Nullable;

import com.google.common.annotations.Beta;
import com.google.common.base.Predicate;
import com.google.inject.ImplementedBy;

/**
 * jclouds needs to understand the difference between resources it creates and
 * those supplied by the user. For example, if jclouds creates a security group,
 * it should be able to delete this as a part of cleanup without accidentally
 * deleting resources the user specified manually.
 * 
 * <h3>uniqueness of a name</h3>
 * 
 * The naming convention must apply to both to resources shared across all
 * members of a group, and those created for a subset of members.
 * 
 * <ol>
 * <li>shared: something shared across all members of a group, and fully
 * retrievable via api. Ex. security group or network</li>
 * <li>unique: something that only applies to individuals or subsets of a group,
 * or isn't fully retrievable via api. Ex. node names or keypair names</li>
 * </ol>
 * 
 * <h4>why repeat?</h4>
 * 
 * Some resources we'd otherwise want to share across a group must be
 * redundantly created, if the user has no access to the complete object via api
 * or otherwise. For example, ssh key apis generally do not store the private
 * key data on the server. In order to ensure we can always log into a server
 * without configuration, we may generate a temporary key on-demand for each
 * call to {@link ComputeService#createNodesInGroup}
 * 
 * 
 * Typically, a security group or network is something shared across all members
 * of a group, so the name should be concise, yet unique.
 * 
 * <h2>implementation</h2>
 * 
 * Typically, a security group or network is something shared across all members
 * of a group, so the name should be concise, yet unique.
 * 
 * <h4>character sets and delimiters</h4>
 * 
 * Character sets in apis are often bound to dns names, perhaps also allowing
 * underscores. Since jclouds groups are allowed to be alphanumeric with hyphens
 * (hostname style), care must be taken to implement this in a way that allows
 * the delimiter to be also nested in the group name itself. In other words, we
 * need to be able to encode a group into a name even when they share the same
 * character set. Note that characters like {@code #} can sometimes break apis.
 * As such, you may end preferring always using a hyphen.
 * 
 * <h4>shared resources</h4>
 * 
 * A good name for a shared resources might include a prefix of jclouds, a
 * delimiter of {@code -} followed by the group name. The jclouds prefix
 * signifies this resource is safe to delete, and the hash clearly delineates
 * the encoding from what's in the group name.
 * 
 * <h3>example</h3>
 * 
 * given a jclouds group named {@code mycluster}, the naming convention for
 * shared resources would produce {@code jclouds-mycluster}
 * 
 * <h4>unique resources</h4>
 * 
 * A good name for a unique resource might be the same as the shared, with a
 * random hex string suffix. A few hex characters can give you 4096
 * combinations, giving a small degree of collision avoidance, yet without
 * making the resource name difficult.
 * 
 * <h3>example</h3>
 * 
 * given a jclouds group named {@code mycluster}, the naming convention for
 * unique resources could produce {@code jclouds-mycluster-f3e} the first time,
 * and {@code jclouds-mycluster-e64} the next.
 * 
 * <h3>note</h3>
 * 
 * It is typically safe to assume that an {@link IllegalStateException} is
 * thrown when attempting to create a named resource which already exists.
 * However, if you attempt to create a resource with a name generated by
 * {@link GroupNamingConvention}, and receive an {@link IllegalStateException},
 * it may have been for another reason besides name conflict.
 * 
 * @author Adrian Cole
 */
@Beta
public interface GroupNamingConvention {

   @ImplementedBy(FormatSharedNamesAndAppendUniqueStringToThoseWhichRepeat.Factory.class)
   public static interface Factory {

      GroupNamingConvention create();

      /**
       * top-level resources do not need a prefix, yet still may need to follow
       * a naming convention
       */
      GroupNamingConvention createWithoutPrefix();

   }

   /**
    * encodes the {code group parameter} into a name that exists only once in
    * the group.
    * 
    */
   String sharedNameForGroup(String group);

   /**
    * encodes the {code group parameter} into a name that exists more than once
    * in the group.
    * 
    * <h3>note</h3>
    * 
    * Do not expect this name to be guaranteed unique, though a good
    * implementation should guess a unique name in one or two tries.
    */
   String uniqueNameForGroup(String group);

   /**
    * retrieve the group associated with the encoded name
    * 
    */
   @Nullable
   String groupInUniqueNameOrNull(String encoded);

   /**
    * retrieve the group associated with the encoded name
    * 
    */
   @Nullable
   String groupInSharedNameOrNull(String encoded);

   /**
    * A predicate that identifies if an input has the given group encoded in it.
    */
   Predicate<String> containsGroup(String group);

   /**
    * A predicate that identifies if an input has any group encoded in it.
    */
   Predicate<String> containsAnyGroup();
   
   /**
    * Extracts the group from a shared/unique name. Or returns null if not in correct format to contain a group.
    */
   String extractGroup(String encoded);
}
