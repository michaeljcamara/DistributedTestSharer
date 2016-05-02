#' FUNCTION: read_data
#'
#' read in the user-specified file

read_data <- function(fname) {
    read.csv(fname)
}

#' FUNCTION: convert_to_milliseconds
#'
#' convert seconds data to milliseconds for consistency

convert_to_milliseconds <- function(data) {
    d <- dplyr::mutate(data,
                  time = time * 1000)
    return(d)
}

#' FUNCTION: filter_tool
#'
#' filter for the different tools present in the tlb data

filter_tool <- function(data, t) {
    d <- dplyr::filter(data, tool == t)
    return(d)
}

#' FUNCTION: calculate_averages
#'
#' calculate average time across trials for visualizing as a histogram

calculate_averages <- function(data) {
    l <- dplyr::distinct(dplyr::select(data, number_nodes))
    d <- data.frame("tool" = character(),
                    "number_nodes" = integer(),
                    "average_time" = double(),
                    "t" = character())
    names(d) <- c("tool", "number_nodes", "average_time", "t")

    for (i in l[[1]]) {
    a <- dplyr::filter(data, i == number_nodes)

    tool <- dplyr::distinct(dplyr::select(a, tool))
    number_nodes <- i
    average_time <- mean(a$time)
    t <- dplyr::distinct(dplyr::select(a,t))

    b <- data.frame(tool, number_nodes, average_time, t)
    names(b) <- c("tool", "number_nodes", "average_time", "t")
    d <- rbind(d, b)

    }
    return(d)
}

#' FUNCTION: visualize_nodes_versus_time
#'
#' compare number of nodes versus time given a set of data

visualize_nodes_versus_time <- function(data) {
    p <- ggplot2::ggplot(data, ggplot2::aes(x = number_nodes, y = time, group = number_nodes)) +
    ggplot2::geom_boxplot() +
    ggplot2::scale_shape(guide = ggplot2::guide_legend(title = ""), solid = FALSE) +
    ggplot2::theme_grey(base_size = 6) +
    ggplot2::theme(axis.text.x = ggplot2::element_text(angle = 45, hjust = 1, size = 20)) +
    ggplot2::theme(axis.text.y = ggplot2::element_text(angle = 45, hjust = 1, size = 20)) +
    ggplot2::ylab("Time (ms)") +
    ggplot2::xlab("Nodes (count)") +
    ggplot2::labs(title = "Apache Ivy Test Suite (TLB)") +
    ggplot2::theme(title = ggplot2::element_text(size=20), legend.position = "top",
                                      panel.background = ggplot2::element_blank())
  return(p)
}

#' FUNCTION: visualize_nodes_versus_time_hist
#'
#' compare number of nodes versus time given a set of data as a histogram

visualize_nodes_versus_time_hist <- function(data) {
    p <- ggplot2::ggplot(data, ggplot2::aes(x = number_nodes, y = average_time)) +
    ggplot2::geom_bar(stat = "identity") +
    ggplot2::scale_shape(guide = ggplot2::guide_legend(title = ""), solid = FALSE) +
    ggplot2::theme_grey(base_size = 6) +
    ggplot2::theme(axis.text.x = ggplot2::element_text(angle = 45, hjust = 1, size = 20)) +
    ggplot2::theme(axis.text.y = ggplot2::element_text(angle = 45, hjust = 1, size = 20)) +
    ggplot2::ylab("Average Time (ms)") +
    ggplot2::xlab("Nodes (count)") +
    ggplot2::labs(title = "SchemaAnalyst Test Suite (TLB)") +
    ggplot2::theme(title = ggplot2::element_text(size=20), legend.position = "top",
                                      panel.background = ggplot2::element_blank())
  return(p)
}

#' FUNCTION: visualize_nodes_versus_time_both
#'
#' compare number of nodes versus time given a set of data
#' for both tools: DTS and TLB

visualize_nodes_versus_time_both <- function(data) {
    p <- ggplot2::ggplot(data, ggplot2::aes(x = number_nodes, y = average_time, colour = t)) +
    ggplot2::geom_line() +
    ggplot2::scale_shape(guide = ggplot2::guide_legend(title = ""), solid = FALSE) +
    ggplot2::theme_grey(base_size = 6) +
    ggplot2::theme(axis.text.x = ggplot2::element_text(angle = 45, hjust = 1, size = 20)) +
    ggplot2::theme(axis.text.y = ggplot2::element_text(angle = 45, hjust = 1, size = 20)) +
    ggplot2::theme(legend.text=ggplot2::element_text(size=20)) +
    ggplot2::ylab("Average Time (ms)") +
    ggplot2::xlab("Nodes (count)") +
    ggplot2::labs(title = "SchemaAnalyst Test Suite",
                  colour = "Tool:") +
    ggplot2::theme(title = ggplot2::element_text(size=20), legend.position = "top",
                                      panel.background = ggplot2::element_blank())
  return(p)
}

#' FUNCTION: visualize_nodes_versus_time_box_both
#'
#' compare number of nodes versus time given a set of data in a boxplot
#' for comparing side-by-side tools

visualize_nodes_versus_time_box_both <- function(data) {
    p <- ggplot2::ggplot(data, ggplot2::aes(x = number_nodes, y = time, group = number_nodes, fill = t)) +
    ggplot2::geom_boxplot() +
    ggplot2::scale_shape(guide = ggplot2::guide_legend(title = ""), solid = FALSE) +
    ggplot2::theme_grey(base_size = 6) +
    ggplot2::theme(axis.text.x = ggplot2::element_text(angle = 45, hjust = 1, size = 20)) +
    ggplot2::theme(axis.text.y = ggplot2::element_text(angle = 45, hjust = 1, size = 20)) +
    ggplot2::ylab("Time (ms)") +
    ggplot2::xlab("Nodes (count)") +
    ggplot2::labs(title = "SchemaAnalyst Test Suite") +
    ggplot2::theme(title = ggplot2::element_text(size=20), legend.position = "top",
                                      panel.background = ggplot2::element_blank())
  return(p)
}
